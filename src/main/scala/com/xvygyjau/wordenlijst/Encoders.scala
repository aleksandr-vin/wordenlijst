package com.xvygyjau.wordenlijst

import cats.effect.IO
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf

object Encoders {
  implicit val failureResponseEncoder: Encoder[FailureResponse] = deriveEncoder
  implicit val failureResponseEntityEncoder: EntityEncoder[IO, FailureResponse] =
    jsonEncoderOf[IO, FailureResponse]
}
