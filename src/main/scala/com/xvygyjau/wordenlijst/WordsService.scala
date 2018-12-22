package com.xvygyjau.wordenlijst

import cats.Eval
import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import github4s.Github
import github4s.Github._
import github4s.GithubResponses.{GHException, GHResult}
import github4s.free.domain.GistFile
import github4s.jvm.Implicits._
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import org.http4s.circe.jsonEncoderOf
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityEncoder, HttpService, _}
import scalaj.http.HttpResponse

class WordsService(auth: Auth)
    extends Http4sDsl[IO]
    with LazyLogging {

  case class AddPhraseResponse(phrase: Option[String], message: String)

  implicit val addPhraseResponseEncoder: Encoder[AddPhraseResponse] =
    deriveEncoder
  implicit val addPhraseResponseEntityEncoder
    : EntityEncoder[IO, AddPhraseResponse] =
    jsonEncoderOf[IO, AddPhraseResponse]

  def createGist(accessToken: github.AccessToken): IO[AddPhraseResponse] = {
    val files = Map(
      "token.scala" -> GistFile("val accessToken = sys.env.get(\"GITHUB4S_ACCESS_TOKEN\")"),
      "gh4s.scala"  -> GistFile("val gh = Github(accessToken)")
    )
    val newGist = Github(Some(accessToken.value)).gists.newGist("Github4s entry point", public = true, files)
    for {
      u1 <- IO.eval(newGist.exec[Eval, HttpResponse[String]]())
      gistResponse = u1 match {
        case Right(GHResult(gist, status, headers)) =>
          logger.debug(
            s"Github response status: $status, user: $gist, headers: $headers")
          AddPhraseResponse(None,
            s"Gist ${gist.url} created")
        case Left(e: GHException) =>
          logger.error(s"Github error: ${e.getMessage}")
          AddPhraseResponse(None, e.getMessage)
      }
    } yield gistResponse
  }

  object PhraseQueryParamMatcher
      extends OptionalQueryParamDecoderMatcher[String]("phrase")

  val authedService: AuthedService[Auth.User, IO] = {
    AuthedService {
      case POST -> Root :? PhraseQueryParamMatcher(maybePhrase) as user =>
        maybePhrase match {
          case None =>
            BadRequest(
              AddPhraseResponse(None, "\"phrase\" query parameter is missing"))
          case Some(phrase) =>
            for {
              response <- createGist(user.accessToken)
              result <- Ok(response.copy(phrase = Some(phrase)))
            } yield result
        }
    }
  }

  val service: HttpService[IO] = auth.middleware(authedService)
}
