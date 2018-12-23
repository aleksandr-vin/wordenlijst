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

class WordsService(auth: Auth) extends Http4sDsl[IO] with LazyLogging {

  case class AddPhraseResponse(phrase: Option[String], message: String)

  implicit val addPhraseResponseEncoder: Encoder[AddPhraseResponse] =
    deriveEncoder
  implicit val addPhraseResponseEntityEncoder
    : EntityEncoder[IO, AddPhraseResponse] =
    jsonEncoderOf[IO, AddPhraseResponse]

  def updateGist(accessToken: github.AccessToken,
                 gistId: String,
                 phrase: String): IO[AddPhraseResponse] = {
    val gist = Github(Some(accessToken.value)).gists.getGist(gistId)
    for {
      u1 <- IO.eval(gist.exec[Eval, HttpResponse[String]]())
      gistResponse = u1 match {
        case Right(GHResult(result, status, headers)) =>
          logger.info(s"Github gist: ${result.url}")
          logger.debug(
            s"Github response status: $status, result: $result, headers: $headers")
          logger.debug(s"Github gist: $result")
          AddPhraseResponse(Some(phrase), s"Gist ${result.url} obtained")
        case Left(e: GHException) =>
          logger.error(s"Github error: ${e.getMessage}")
          AddPhraseResponse(None, e.getMessage)
      }
      // TODO: append phrase to gist
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
              response <- updateGist(user.accessToken, user.gistId, phrase)
              result <- Ok(response.copy(phrase = Some(phrase)))
            } yield result
        }
    }
  }

  val service: HttpService[IO] = auth.middleware(authedService)
}
