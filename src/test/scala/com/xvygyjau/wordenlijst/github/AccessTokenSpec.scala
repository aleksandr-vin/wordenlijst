package com.xvygyjau.wordenlijst.github

import org.pico.hashids.Hashids
import org.specs2.mutable.Specification

class AccessTokenSpec extends Specification {
  "This is a specification for the github.AccessToken module".txt

  implicit val hashids: Hashids = Hashids.reference("test")

  val at = AccessToken("cb213d0c3c98e33730862234d414c040d1c188df")

  "The github.AccessToken should" >> {
    "encode" >> {
      val hash = AccessToken.encode(at)
      hash must not equalTo at.value
      hash.length must not be greaterThan(40)
    }
    "decode" >> {
      val hash = AccessToken.encode(at)
      val at2 = AccessToken.decode(hash)
      at2 must be equalTo at
    }
    "safely print" >> {
      at.toString must be equalTo "cb21********************************88df"
      AccessToken("12").toString must be equalTo "**"
      AccessToken("123").toString must be equalTo "***"
      AccessToken("1234").toString must be equalTo "1**4"
      AccessToken("12345").toString must be equalTo "1***5"
    }
    "require non-empty value" >> {
      AccessToken("") must throwAn[IllegalArgumentException]
    }
  }
}
