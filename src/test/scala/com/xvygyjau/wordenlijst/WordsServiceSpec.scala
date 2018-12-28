package com.xvygyjau.wordenlijst

import cats.data.{Kleisli, OptionT}
import cats.effect.IO
import cats.free.Free
import com.xvygyjau.wordenlijst.testdata.valid
import github4s.GHGists
import github4s.GithubResponses.{GHResponse, GHResult}
import github4s.app.GitHub4s
import github4s.free.algebra.{GistOps, UserOps}
import github4s.free.domain.{EditGistFile, Gist, GistFile}
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.server.AuthMiddleware
import org.pico.hashids.Hashids
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, Matchers}

class WordsServiceSpec extends FlatSpec with MockFactory with Matchers {

  {
    lazy val result = savePhrase.as[String].unsafeRunSync()

    it should "call gist api with proper parameters and return expected values" in {
      result should include("New phrase added")
      result should include(s""""value":"${valid.newPhrase}"""")
    }
  }

  class GistOpsTest extends GistOps[GitHub4s]
  class UserOpsTest extends UserOps[GitHub4s]

  private[this] val savePhrase: Response[IO] = {
    implicit val hashids: Hashids = Hashids.reference(valid.hashidsSalt)

    val gistOps = mock[GistOpsTest]
    (gistOps.getGist _)
      .expects(valid.Gist.id, None, Some(valid.accessToken.value))
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

    (gistOps.editGist _)
      .expects(valid.Gist.id,
        valid.Gist.description,
        Map(valid.Gist.filename → Some(EditGistFile(valid.Gist.fileContent + s"${valid.newPhrase}\n"))),
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

    class TestAuth extends Auth with Http4sDsl[IO] {

      val authUser: Kleisli[IO, Request[IO], Either[String, Auth.User]] =
        Kleisli({ request =>
          IO.pure(
            Right(Auth.User(valid.accessToken, valid.Gist.id)): Either[
              String,
              Auth.User])
        })

      val onFailure: AuthedService[String, IO] = Kleisli(
        req => OptionT.liftF(Forbidden(req.authInfo)))

      override val middleware: AuthMiddleware[IO, Auth.User] =
        AuthMiddleware(authUser, onFailure)
    }

    val request =
      Request[IO](Method.POST, Uri.unsafeFromString(s"?phrase=${valid.newPhrase.replace(' ', '+')}"))

    val auth = new TestAuth
    new WordsService(
      auth,
      _ => new GHGists(Some(valid.accessToken.value))(gistOps)).service
      .orNotFound(request)
      .unsafeRunSync()
  }
}
