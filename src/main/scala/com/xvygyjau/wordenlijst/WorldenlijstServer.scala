package com.xvygyjau.wordenlijst

import cats.effect.{Effect, IO}
import fs2.StreamApp
import org.http4s.server.blaze.BlazeBuilder

import scala.concurrent.ExecutionContext

object WorldenlijstServer extends StreamApp[IO] {
  import scala.concurrent.ExecutionContext.Implicits.global

  def stream(args: List[String], requestShutdown: IO[Unit]) = ServerStream.stream[IO]
}

object ServerStream {

  import Ids.hashids

  def githubService[F[_]: Effect] = new GithubService[F].service

  val port = sys.env.getOrElse("PORT", "8080").toInt

  def stream[F[_]: Effect](implicit ec: ExecutionContext) =
    BlazeBuilder[F]
      .bindHttp(port, "0.0.0.0")
      .mountService(githubService, "/github/")
      .serve
}
