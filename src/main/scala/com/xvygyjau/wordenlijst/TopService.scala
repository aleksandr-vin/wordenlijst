package com.xvygyjau.wordenlijst

import cats.effect.IO
import org.http4s.{HttpService, Uri}
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Location

class TopService extends Http4sDsl[IO] {

  val service: HttpService[IO] = {
    HttpService[IO] {
      case GET -> Root =>
        TemporaryRedirect(Location(Uri.uri("https://github.com/aleksandr-vin/wordenlijst/blob/master/README.md#wordenlijst")))
    }
  }
}
