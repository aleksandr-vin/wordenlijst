package com.xvygyjau.wordenlijst

import cats.effect.IO
import fs2.StreamApp
import org.http4s.server.blaze.BlazeBuilder

import scala.concurrent.ExecutionContext

object WorldenlijstServer extends StreamApp[IO] {
  import scala.concurrent.ExecutionContext.Implicits.global

  def stream(args: List[String], requestShutdown: IO[Unit]) = ServerStream.stream
}

object ServerStream {

  import Ids.hashids

  val auth = new CookieAuth()

  def githubService = new GithubService().service
  def wordsService = new WordsService(auth).service
  def healthService = new HealthService().service

  val port = sys.env.getOrElse("PORT", "8080").toInt

  def stream(implicit ec: ExecutionContext) =
    BlazeBuilder[IO]
      .bindHttp(port, "0.0.0.0")
      .mountService(githubService, "/github")
      .mountService(wordsService, "/words")
      .mountService(healthService, "/health")
      .serve
}
