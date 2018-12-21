package com.xvygyjau.wordenlijst

import cats.effect.IO
import org.http4s.HttpService
import org.http4s.dsl.Http4sDsl

class HealthService extends Http4sDsl[IO] {

  val service: HttpService[IO] = {
    HttpService[IO] {
      case GET -> Root =>
        Ok(BuildInfo.toJson)
    }
  }
}
