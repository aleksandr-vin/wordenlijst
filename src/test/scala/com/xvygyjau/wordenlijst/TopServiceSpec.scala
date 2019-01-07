package com.xvygyjau.wordenlijst

import cats.effect.IO
import org.http4s.headers.Location
import org.http4s.implicits._
import org.http4s.{Method, Request, Response, Status, Uri}
import org.scalatest.Matchers

class TopServiceSpec extends BaseSpec with Matchers {

  {
    lazy val result = getRoot

    it should "redirect" in {
      getRoot.status should be(Status.TemporaryRedirect)
    }

    it should "redirect to master README on github.com/aleksandr-vin/wordenlijst" in {
      val header: Option[Location] = result.headers.get(Location)
      header shouldBe defined
      header.get.value should be("https://github.com/aleksandr-vin/wordenlijst/blob/master/README.md#wordenlijst")
    }
  }

  private[this] val getRoot: Response[IO] = {
    val get = Request[IO](
      Method.GET,
      Uri.unsafeFromString(s"/"))

    new TopService().service
      .orNotFound(get)
      .unsafeRunSync()
  }
}
