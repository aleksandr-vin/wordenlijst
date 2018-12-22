package com.xvygyjau.wordenlijst

import cats.data._
import cats.effect.IO
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import com.xvygyjau.wordenlijst.github.AccessToken
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import org.http4s.circe.jsonEncoderOf
import org.http4s.dsl.Http4sDsl
import org.http4s.server._
import org.http4s.{EntityEncoder, HttpService, _}
import org.pico.hashids.Hashids

class WordsService(implicit hashids: Hashids)
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

  case class User(accessToken: github.AccessToken)

  case class ForiddenResponse(message: String)

  implicit val ForiddenResponseEncoder: Encoder[ForiddenResponse] =
    deriveEncoder
  implicit val ForiddenResponseEntityEncoder
    : EntityEncoder[IO, ForiddenResponse] =
    jsonEncoderOf[IO, ForiddenResponse]

  val authUser: Kleisli[IO, Request[IO], Either[ForiddenResponse, User]] =
    Kleisli({ request =>
      IO(
        (for {
          header <- headers.Cookie
            .from(request.headers)
            .toRight("Cookie not found or it was a parsing error")
          cookie <- header.values.toList
            .find(_.name == "apiKey")
            .toRight("Couldn't find the apiKey in cookie")
          hash = cookie.content
          accessToken <- Either
            .catchOnly[IllegalArgumentException](AccessToken.decode(hash))
            .leftMap(_ => "Couldn't decode access token")
        } yield User(accessToken)).leftMap(ForiddenResponse.apply)
      )
    })

  val onFailure: AuthedService[ForiddenResponse, IO] = Kleisli(
    req => OptionT.liftF(Forbidden(req.authInfo)))

  val middleware = AuthMiddleware(authUser, onFailure)

  val authedService: AuthedService[User, IO] = {
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

  val service: HttpService[IO] = middleware(authedService)
}
