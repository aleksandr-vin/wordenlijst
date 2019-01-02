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
import org.http4s.{
  AuthedService,
  BasicCredentials,
  EntityEncoder,
  Headers,
  Message,
  Request,
  headers
}
import org.pico.hashids.Hashids
import java.util.Base64
import java.nio.charset.StandardCharsets

import org.http4s.Credentials.AuthParams

trait Auth {
  val middleware: AuthMiddleware[IO, User]
}

class CombinedAuth(implicit hashids: Hashids) extends Auth with Http4sDsl[IO] {

  implicit val ForiddenResponseEncoder: Encoder[ForiddenResponse] =
    deriveEncoder
  implicit val ForiddenResponseEntityEncoder
    : EntityEncoder[IO, ForiddenResponse] =
    jsonEncoderOf[IO, ForiddenResponse]

  def byCookie(requestHeaders: Headers): Either[String, User] =
    for {
      header <- headers.Cookie
        .from(requestHeaders)
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
    } yield User(accessToken, gistId)

  def byBasicCredentials(requestHeaders: Headers): Either[String, User] =
    for {
      header <- headers.Authorization
        .from(requestHeaders)
        .toRight("Authorization not found or it was a parsing error")
      basicCreds <- BasicCredentials
        .unapply(header.credentials)
        .toRight("Couldn't decode basic credentials")
      apiKey = basicCreds.password
      accessToken <- Either
        .catchOnly[IllegalArgumentException](AccessToken.decode(apiKey))
        .leftMap(_ => "Couldn't decode access token")
      gistId = basicCreds.username
    } yield User(accessToken, gistId)

  def byAllMeans(requestHeaders: Headers): Either[String, User] =
    byCookie(requestHeaders) orElse byBasicCredentials(requestHeaders)

  val authUser: Kleisli[IO, Request[IO], Either[ForiddenResponse, User]] =
    Kleisli({ request: Message[IO] =>
      IO(byAllMeans(request.headers).leftMap(ForiddenResponse.apply))
    })

  val onFailure: AuthedService[ForiddenResponse, IO] = Kleisli(
    req => OptionT.liftF(Forbidden(req.authInfo)))

  override val middleware: AuthMiddleware[IO, User] =
    AuthMiddleware(authUser, onFailure)
}

object Auth {
  case class User(accessToken: github.AccessToken, gistId: String)

  case class ForiddenResponse(message: String)
}
