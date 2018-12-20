package com.xvygyjau.wordenlijst

import cats.effect.Effect
import com.xvygyjau.wordenlijst.github.AccessToken
import io.circe.Json
import org.http4s.HttpService
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

import scala.util.{Failure, Success, Try}

class GithubService[F[_]: Effect] extends Http4sDsl[F] {

  val service: HttpService[F] = {
    HttpService[F] {
      case GET -> Root / "token" / token =>
        Try {
          import Ids.hashids
          val accessToken = github.AccessToken(token)
          AccessToken.encode(accessToken)
        } match {
          case Success(apiKey) =>
            Ok(
              Json.obj(
                "message" -> Json.fromString(
                  s"Hello, unknown Github user, your api key is $apiKey"),
                "apiKey" -> Json.fromString(apiKey)))
          case Failure(exception) =>
            InternalServerError(Json.obj(
              "message" -> Json.fromString(exception.getMessage)))
        }
    }
  }
}
