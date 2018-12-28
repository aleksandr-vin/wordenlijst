package com.xvygyjau.wordenlijst

import cats.effect.IO
import cats.free.Free
import com.xvygyjau.wordenlijst.testdata.valid
import github4s.GithubResponses.{GHResponse, GHResult}
import github4s.app.GitHub4s
import github4s.free.domain.{Gist, GistFile, User}
import github4s.{GHGists, GHUsers}
import org.http4s._
import org.http4s.implicits._
import org.pico.hashids.Hashids
import org.scalamock.scalatest.MockFactory
import org.scalatest.Matchers

class GithubServiceSpec extends BaseSpec with MockFactory with Matchers {

  {
    lazy val result = retToken.as[String].unsafeRunSync()

    it should "return 200" in {
      retToken.status should be(Status.Ok)
    }

    it should "return api key and gist id" in {
      result should include(valid.User.name.get)
      result should include(s""""apiKey":"${valid.apiKey}"""")
      result should include(s""""gistId":"${valid.Gist.id}"""")
    }
  }

  private[this] val retToken: Response[IO] = {
    implicit val hashids: Hashids = Hashids.reference(valid.hashidsSalt)

    val gistOps = mock[GistOpsTest]
    (gistOps.newGist _)
      .expects(valid.Gist.description,
               valid.Gist.public,
               Map(valid.Gist.filename → GistFile(valid.Gist.fileContent)),
               Some(valid.accessToken.value))
      .returns {
        val gist =
          Gist(valid.Gist.url,
               valid.Gist.id,
               valid.Gist.description,
               valid.Gist.public,
               Map(valid.Gist.filename → GistFile(valid.Gist.fileContent)))
        val response: Free[GitHub4s, GHResponse[Gist]] =
          Free.pure(Right(GHResult(gist, 200, Map.empty)))
        response
      }

    val userOps = mock[UserOpsTest]
    (userOps.getAuthUser _)
      .expects(Some(valid.accessToken.value))
      .returns {
        val user =
          User(
            valid.User.id,
            valid.User.login,
            valid.User.avatarUrl,
            valid.User.htmlUrl,
            valid.User.name
          )
        val response: Free[GitHub4s, GHResponse[User]] =
          Free.pure(Right(GHResult(user, 200, Map.empty)))
        response
      }

    val getApiKey = Request[IO](
      Method.GET,
      Uri.unsafeFromString(s"/token/${valid.accessToken.value}"))

    new GithubService(
      _ => new GHUsers(Some(valid.accessToken.value))(userOps),
      _ => new GHGists(Some(valid.accessToken.value))(gistOps)).service
      .orNotFound(getApiKey)
      .unsafeRunSync()
  }
}
