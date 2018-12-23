package com.xvygyjau.wordenlijst

import cats.data.{Kleisli, OptionT}
import cats.effect.IO
import cats.implicits._
import com.xvygyjau.wordenlijst.Auth.{ForiddenResponse, User}
import com.xvygyjau.wordenlijst.github.AccessToken
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import org.http4s.circe.jsonEncoderOf
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware
import org.http4s.{AuthedService, EntityEncoder, Request, headers}
import org.pico.hashids.Hashids

trait Auth {
  val middleware: AuthMiddleware[IO, User]
}

class CookieAuth(implicit hashids: Hashids) extends Auth with Http4sDsl[IO] {

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
          apiKeyCookie <- header.values.toList
            .find(_.name == "apiKey")
            .toRight("Couldn't find the apiKey in cookie")
          apiKey = apiKeyCookie.content
          accessToken <- Either
            .catchOnly[IllegalArgumentException](AccessToken.decode(apiKey))
            .leftMap(_ => "Couldn't decode access token")
          gistIdCookie <- header.values.toList
            .find(_.name == "gistId")
            .toRight("Couldn't find the gistId in cookie")
          gistId = gistIdCookie.content
        } yield User(accessToken, gistId)).leftMap(ForiddenResponse.apply)
      )
    })

  val onFailure: AuthedService[ForiddenResponse, IO] = Kleisli(
    req => OptionT.liftF(Forbidden(req.authInfo)))

  override val middleware: AuthMiddleware[IO, User] = AuthMiddleware(authUser, onFailure)
}

object Auth {
  case class User(accessToken: github.AccessToken, gistId: String)

  case class ForiddenResponse(message: String)
}