package com.xvygyjau.wordenlijst

import cats.effect.IO
import org.http4s._
import org.http4s.implicits._
import org.pico.hashids.Hashids
import org.specs2.matcher.MatchResult

class GithubServiceSpec extends org.specs2.mutable.Specification {

  "GithubService" >> {
    "return 200" >> {
      uriReturns200()
    }
    "return api key" >> {
      uriReturnsApiKey()
    }
  }

  private[this] val retHelloWorld: Response[IO] = {
    implicit val hashids: Hashids = Hashids.reference("test")
    val getHW = Request[IO](Method.GET, Uri.uri("/token/cb213d0c3c98e33730862234d414c040d1c188df"))
    new GithubService[IO].service.orNotFound(getHW).unsafeRunSync()
  }

  private[this] def uriReturns200(): MatchResult[Status] =
    retHelloWorld.status must beEqualTo(Status.Ok)

  private[this] def uriReturnsApiKey(): MatchResult[String] =
    retHelloWorld.as[String].unsafeRunSync() must beEqualTo("{\"message\":\"Hello, unknown Github user, your api key is l80M3SMbNPJXow9tQVwl8jRq3FX76MnZNb4\",\"apiKey\":\"l80M3SMbNPJXow9tQVwl8jRq3FX76MnZNb4\"}")
}
