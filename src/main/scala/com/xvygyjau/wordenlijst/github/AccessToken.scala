package com.xvygyjau.wordenlijst.github

import org.pico.hashids.Hashids

case class AccessToken(value: String) {
  override def toString: String = {
    val n = math.min(4, value.length / 4)
    value.take(n) + value.drop(n).dropRight(n).map(_ => '*') + value.takeRight(n)
  }
}

object AccessToken {

  def apply(string: String) = {
    require(string.nonEmpty, "Empty access token")
    new AccessToken(string.toLowerCase)
  }

  def encode(accessToken: AccessToken)(implicit hashids: Hashids): String = {
    hashids.encodeHex(accessToken.value)
  }

  def decode(string: String)(implicit hashids: Hashids): AccessToken = {
    AccessToken(hashids.decodeHex(string))
  }
}
