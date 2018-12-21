package com.xvygyjau.wordenlijst

import cats.Eval
import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import com.xvygyjau.wordenlijst.github.AccessToken
import github4s.Github
import github4s.Github._
import github4s.GithubResponses.{GHException, GHResult}
import github4s.jvm.Implicits._
import io.circe.Encoder
import io.circe.generic.semiauto._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityEncoder, HttpService}
import org.pico.hashids.Hashids
import scalaj.http.HttpResponse

class GithubService(implicit hashids: Hashids)
    extends Http4sDsl[IO]
    with LazyLogging {

  case class ApiKeyResponse(apiKey: Option[String], message: String)

  implicit val apiKeyResponseEncoder: Encoder[ApiKeyResponse] = deriveEncoder
  implicit val apiKeyResponseEntityEncoder: EntityEncoder[IO, ApiKeyResponse] =
    jsonEncoderOf[IO, ApiKeyResponse]

  def getApiKey(accessToken: github.AccessToken): IO[ApiKeyResponse] = {
    val userRequest = Github(Some(accessToken.value)).users.getAuth
    for {
      u1 <- IO.eval(userRequest.exec[Eval, HttpResponse[String]]())
      apiKeyResponse = u1 match {
        case Right(GHResult(user, status, headers)) =>
          logger.info(s"Github user: ${user.login}")
          logger.debug(s"Github response status: $status, user: $user, headers: $headers")
          val hash = AccessToken.encode(accessToken)
          val userName = user.name.getOrElse(user.login)
          ApiKeyResponse(Some(hash), s"Welcome $userName, your api key is $hash")
        case Left(e: GHException) =>
          logger.error(s"Github error: ${e.getMessage}")
          ApiKeyResponse(None, e.getMessage)
      }
    } yield apiKeyResponse
  }

  val service: HttpService[IO] = {
    HttpService[IO] {
      case GET -> Root / "token" / token =>
        for {
          apiKey <- getApiKey(github.AccessToken(token))
          result <- Ok(apiKey)
        } yield result
    }
  }
}
