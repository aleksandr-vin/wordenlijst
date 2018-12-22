package com.xvygyjau.wordenlijst

import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import org.http4s.circe.jsonEncoderOf
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityEncoder, HttpService, _}

class WordsService(auth: Auth)
    extends Http4sDsl[IO]
    with LazyLogging {

  case class AddPhraseResponse(phrase: Option[String], message: String)

  implicit val addPhraseResponseEncoder: Encoder[AddPhraseResponse] =
    deriveEncoder
  implicit val addPhraseResponseEntityEncoder
    : EntityEncoder[IO, AddPhraseResponse] =
    jsonEncoderOf[IO, AddPhraseResponse]

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
            Ok(
              AddPhraseResponse(
                Some(phrase),
                s"Dear user with Github access token ${user.accessToken}, your phrase should be added"))
        }
    }
  }

  val service: HttpService[IO] = auth.middleware(authedService)
}
