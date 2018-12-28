package com.xvygyjau.wordenlijst.github

import org.pico.hashids.Hashids
import org.scalatest.{FlatSpec, Matchers}
import com.xvygyjau.wordenlijst.testdata.valid

class AccessTokenSpec extends FlatSpec with Matchers {

  implicit val hashids: Hashids = Hashids.reference(valid.hashidsSalt)

  it should "encode" in {
    val hash = AccessToken.encode(valid.accessToken)
    hash shouldNot be(valid.accessToken.value)
    hash.length shouldNot be > 40
  }

  it should "decode" in {
    val hash = AccessToken.encode(valid.accessToken)
    val at2 = AccessToken.decode(hash)
    at2 should be(valid.accessToken)
  }

  it should "safely print" in {
    valid.accessToken.toString should be(
      "cb21********************************88df")
    AccessToken("12").toString should be("**")
    AccessToken("123").toString should be("***")
    AccessToken("1234").toString should be("1**4")
    AccessToken("12345").toString should be("1***5")
  }

  it should "require non-empty value" in {
    an[IllegalArgumentException] should be thrownBy AccessToken("")
  }
}
