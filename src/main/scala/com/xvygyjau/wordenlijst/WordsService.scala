package com.xvygyjau.wordenlijst

import cats.Eval
import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import com.xvygyjau.wordenlijst.FailureResponse.Response
import github4s.{GHGists, Github}
import github4s.Github._
import github4s.GithubResponses.{GHException, GHResult}
import github4s.free.domain.EditGistFile
import github4s.jvm.Implicits._
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import org.http4s.circe.jsonEncoderOf
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityEncoder, HttpService, _}
import scalaj.http.HttpResponse
import com.xvygyjau.wordenlijst.Encoders._

class WordsService(auth: Auth, gists: github.AccessToken => GHGists)
    extends Http4sDsl[IO]
    with LazyLogging {

  case class AddPhraseResponse(phrase: Phrase, message: String)

  def updateGist(accessToken: github.AccessToken,
                 gistId: String,
                 phrase: Phrase): IO[Response[AddPhraseResponse]] = {
    val gistsRepo = gists(accessToken)
    val gist = gistsRepo.getGist(gistId)
    for {
      u1 <- IO.eval(gist.exec[Eval, HttpResponse[String]]())
      gistResponse <- u1 match {
        case Right(GHResult(result, status, headers)) =>
          logger.info(s"Github gist: ${result.url}")
          logger.debug(
            s"Github response status: $status, result: $result, headers: $headers")
          logger.debug(s"Github gist: $result")
          val files = Map(
            "wordenlijst" -> Some(
              EditGistFile(
                result.files("wordenlijst").content + phrase.value + "\n"))
          )
          val updatedGist = gistsRepo
            .editGist(gistId, result.description, files)
          IO.eval(updatedGist.exec[Eval, HttpResponse[String]]()).flatMap {
            case Right(GHResult(result, status, headers)) =>
              logger.info(s"Updatd Github gist ${result.url}")
              logger.debug(
                s"Github response status: $status, result: $result, headers: $headers")
              IO.pure(
                Right(
                  AddPhraseResponse(phrase,
                                    s"New phrase added to gist ${result.url}")))
            case Left(e: GHException) =>
              logger.error(s"Github error: ${e.getMessage}")
              IO.pure(Left(FailureResponse(e.getMessage)))
          }
        case Left(e: GHException) =>
          logger.error(s"Github error: ${e.getMessage}")
          IO.pure(Left(FailureResponse(e.getMessage)))
      }
    } yield gistResponse
  }

  case class Phrase(value: String)

  object Phrase {
    def apply(string: String): Phrase = {
      new Phrase(string.replaceAll("\n", " "))
    }
  }

  implicit val fooDecoder: QueryParamDecoder[Phrase] =
    QueryParamDecoder[String].map(Phrase.apply)

  object PhraseQueryParamMatcher
      extends OptionalQueryParamDecoderMatcher[Phrase]("phrase")

  implicit val phraseEncoder: Encoder[Phrase] =
    deriveEncoder

  implicit val addPhraseResponseEncoder: Encoder[AddPhraseResponse] =
    deriveEncoder
  implicit val addPhraseResponseEntityEncoder
    : EntityEncoder[IO, AddPhraseResponse] =
    jsonEncoderOf[IO, AddPhraseResponse]

  val authedService: AuthedService[Auth.User, IO] = {
    AuthedService {
      case POST -> Root :? PhraseQueryParamMatcher(maybePhrase) as user =>
        maybePhrase match {
          case None =>
            BadRequest(FailureResponse("\"phrase\" query parameter is missing"))
          case Some(phrase) =>
            for {
              response <- updateGist(user.accessToken, user.gistId, phrase)
              result <- response match {
                case Left(r)  => Ok(r)
                case Right(r) => Ok(r)
              }
            } yield result
        }
    }
  }

  val service: HttpService[IO] = auth.middleware(authedService)
}
