package com.xvygyjau.wordenlijst.github

import org.pico.hashids.Hashids

case class AccessToken(value: String)

object AccessToken {

  def apply(string: String) = {
    new AccessToken(string.toLowerCase)
  }

  def encode(accessToken: AccessToken)(implicit hashids: Hashids): String = {
    hashids.encodeHex(accessToken.value)
  }

  def decode(string: String)(implicit hashids: Hashids): AccessToken = {
    AccessToken(hashids.decodeHex(string))
  }
}
