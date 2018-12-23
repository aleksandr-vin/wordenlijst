package com.xvygyjau.wordenlijst

case class FailureResponse(message: String)

object FailureResponse {
  type Response[A] = Either[FailureResponse, A]
}