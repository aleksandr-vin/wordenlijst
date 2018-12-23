package com.xvygyjau.wordenlijst

import cats.Eval
import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import com.xvygyjau.wordenlijst.FailureResponse.Response
import com.xvygyjau.wordenlijst.github.AccessToken
import github4s.Github
import github4s.Github._
import github4s.GithubResponses.{GHException, GHResult}
import github4s.free.domain.GistFile
import github4s.jvm.Implicits._
import io.circe.Encoder
import io.circe.generic.semiauto._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{Cookie, EntityEncoder, HttpService}
import org.pico.hashids.Hashids
import scalaj.http.HttpResponse
import com.xvygyjau.wordenlijst.Encoders._

class GithubService(implicit hashids: Hashids)
    extends Http4sDsl[IO]
    with LazyLogging {

  case class TokenResponse(apiKey: String, gistId: String, message: String)

  def getApiKey(
      accessToken: github.AccessToken,
      privateGist: Option[Boolean],
      gistDescription: Option[String]): IO[Response[TokenResponse]] = {
    val userRequest = Github(Some(accessToken.value)).users.getAuth
    val files = Map(
      "wordenlijst" -> GistFile("+++\n")
    )
    val newGist = Github(Some(accessToken.value)).gists
      .newGist(gistDescription.getOrElse("Wordenlijst"),
               public = !privateGist.getOrElse(false),
               files)

    IO.eval(userRequest.exec[Eval, HttpResponse[String]]()).flatMap {
      case Right(GHResult(result, status, headers)) =>
        logger.info(s"Github user ${result.login}")
        logger.debug(
          s"Github response status: $status, result: $result, headers: $headers")
        val apiKey = AccessToken.encode(accessToken)
        val userName = result.name.getOrElse(result.login)
        IO.eval(newGist.exec[Eval, HttpResponse[String]]()).flatMap {
          case Right(GHResult(result, status, headers)) =>
            logger.info(s"New Github gist ${result.url}")
            logger.debug(
              s"Github response status: $status, result: $result, headers: $headers")
            IO.pure(Right(TokenResponse(
              apiKey,
              result.id,
              s"Welcome $userName, your api key is $apiKey, and gist is ${result.url}")))
          case Left(e: GHException) =>
            logger.error(s"Github error: ${e.getMessage}")
            IO.pure(Left(FailureResponse(e.getMessage)))
        }
      case Left(e: GHException) =>
        logger.error(s"Github error: ${e.getMessage}")
        IO.pure(Left(FailureResponse(e.getMessage)))
    }
  }

  implicit val tokenResponseEncoder: Encoder[TokenResponse] = deriveEncoder
  implicit val tokenResponseEntityEncoder: EntityEncoder[IO, TokenResponse] =
    jsonEncoderOf[IO, TokenResponse]

  object HttpOnlyQueryParamMatcher
      extends OptionalQueryParamDecoderMatcher[Boolean]("httpOnly")

  object SecureQueryParamMatcher
      extends OptionalQueryParamDecoderMatcher[Boolean]("secure")

  object PrivateGistQueryParamMatcher
      extends OptionalQueryParamDecoderMatcher[Boolean]("privateGist")

  object gistDescriptionQueryParamMatcher
      extends OptionalQueryParamDecoderMatcher[String]("gistDescription")

  val service: HttpService[IO] = {
    HttpService[IO] {
      case GET -> Root / "token" / token :? HttpOnlyQueryParamMatcher(httpOnly) +& SecureQueryParamMatcher(
            secure) +& PrivateGistQueryParamMatcher(privateGist) +& gistDescriptionQueryParamMatcher(
            gistDescription) =>
        for {
          response <- getApiKey(github.AccessToken(token),
                                privateGist,
                                gistDescription)
          result <- response match {
            case Left(r) =>
              Ok(r).removeCookie("apiKey").removeCookie("gistId")
            case Right(r) =>
              Ok(r)
                .addCookie(
                  Cookie("apiKey",
                         r.apiKey,
                         path = Some("/"),
                         httpOnly = httpOnly.getOrElse(true),
                         secure = secure.getOrElse(true)))
                .addCookie(
                  Cookie("gistId",
                         r.gistId,
                         path = Some("/"),
                         httpOnly = httpOnly.getOrElse(true),
                         secure = secure.getOrElse(true)))
          }
        } yield result
    }
  }
}
