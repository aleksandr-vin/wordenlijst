package com.xvygyjau.wordenlijst

import cats.Eval
import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import com.xvygyjau.wordenlijst.github.AccessToken
import github4s.Github
import github4s.Github._
import github4s.GithubResponses.{GHIO, GHResponse, GHResult}
import github4s.free.domain.User
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

  implicit val apiKeyEncoder: Encoder[ApiKeyResponse] = deriveEncoder
  implicit val apiKeyEntityEncoder: EntityEncoder[IO, ApiKeyResponse] =
    jsonEncoderOf[IO, ApiKeyResponse]

  def getApiKey(accessToken: github.AccessToken): IO[ApiKeyResponse] = IO {

    val user: GHIO[GHResponse[User]] = Github(Some(accessToken.value)).users.getAuth

    val u1: GHResponse[User] = user.exec[Eval, HttpResponse[String]]().value
    u1 match {
      case Right(GHResult(result, status, headers)) =>
        logger.info(s"Github user: ${result.login}")
        val hash = AccessToken.encode(accessToken)
        ApiKeyResponse(Some(hash), s"Welcome, ${result.login}, your api key is $hash")
      case Left(e) =>
        logger.error(s"Github error: ${e.getMessage}")
        ApiKeyResponse(None, e.getMessage)
    }
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
