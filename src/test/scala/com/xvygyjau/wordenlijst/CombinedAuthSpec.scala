package com.xvygyjau.wordenlijst

import com.xvygyjau.wordenlijst.Auth.User
import com.xvygyjau.wordenlijst.testdata.valid
import org.http4s.{BasicCredentials, Header, Headers}
import org.pico.hashids.Hashids
import org.scalatest.{FlatSpec, Matchers}
import java.util.Base64
import java.nio.charset.StandardCharsets

class CombinedAuthSpec extends FlatSpec with Matchers {

  {
    implicit val hashids: Hashids = Hashids.reference(valid.hashidsSalt)

    val auth = new CombinedAuth()

    "byCookie method" should "authenticate by complete cookie (apiKey and gistId)" in {
      val headers = Headers(
        Header("Cookie", s"apiKey=${valid.apiKey}; gistId=${valid.Gist.id}"))

      val r = auth.byCookie(headers)
      r should be('right)
      r.right.get should be(User(valid.accessToken, valid.Gist.id))
    }

    it should "fail on invalid apiKey in cookie" in {
      val headers =
        Headers(Header("Cookie", s"apiKey=XXxxxXXX; gistId=${valid.Gist.id}"))

      val r = auth.byCookie(headers)
      r should be('left)
      r.left.get should be("Couldn't decode access token")
    }

    it should "fail on incomplete cookie (without gistId)" in {
      val headers = Headers(Header("Cookie", s"apiKey=${valid.apiKey}"))

      val r = auth.byCookie(headers)
      r should be('left)
      r.left.get should be("Couldn't find the gistId in cookie")
    }

    it should "fail on incomplete cookie (without apiKey)" in {
      val headers = Headers(Header("Cookie", s"gistId=${valid.Gist.id}"))

      val r = auth.byCookie(headers)
      r should be('left)
      r.left.get should be("Couldn't find the apiKey in cookie")
    }

    it should "fail without cookie" in {
      val headers = Headers(Header("Cookiez", s"zzz"))

      val r = auth.byCookie(headers)
      r should be('left)
      r.left.get should be("Cookie not found or it was a parsing error")
    }

    "byBasicCredentials method" should "authenticate by Basic Authorization" in {
      val creds = BasicCredentials(valid.Gist.id, valid.apiKey)
      val headers = Headers(Header("Authorization", s"Basic ${creds.token}"))

      val r = auth.byBasicCredentials(headers)
      r should be('right)
      r.right.get should be(User(valid.accessToken, valid.Gist.id))
    }

    it should "fail if Authorization scheme is not Basic" in {
      val creds = BasicCredentials(valid.Gist.id, valid.apiKey)
      val headers = Headers(Header("Authorization", s"Bearer ${creds.token}"))

      val r = auth.byBasicCredentials(headers)
      r should be('left)
      r.left.get should be("Couldn't decode basic credentials")
    }

    it should "fail if Basic Authorization has invalid apiKey" in {
      val creds = BasicCredentials(valid.Gist.id, "xxxXXXxxx")
      val headers = Headers(Header("Authorization", s"Basic ${creds.token}"))

      val r = auth.byBasicCredentials(headers)
      r should be('left)
      r.left.get should be("Couldn't decode access token")
    }

    it should "fail if Basic Authorization is not a gistId:apiKey" in {
      val bytes = "123".getBytes(StandardCharsets.ISO_8859_1)
      val token = Base64.getEncoder.encodeToString(bytes)
      val headers = Headers(Header("Authorization", s"Basic $token"))

      val r = auth.byBasicCredentials(headers)
      r should be('left)
      r.left.get should be("Couldn't decode access token")
    }

    "byAllMeans method" should "authenticate by Basic Authorization" in {
      val creds = BasicCredentials(valid.Gist.id, valid.apiKey)
      val headers = Headers(Header("Authorization", s"Basic ${creds.token}"))

      val r = auth.byAllMeans(headers)
      r should be('right)
      r.right.get should be(User(valid.accessToken, valid.Gist.id))
    }

    it should "authenticate by complete cookie (apiKey and gistId)" in {
    val headers = Headers(
      Header("Cookie", s"apiKey=${valid.apiKey}; gistId=${valid.Gist.id}"))

    val r = auth.byAllMeans(headers)
    r should be('right)
    r.right.get should be(User(valid.accessToken, valid.Gist.id))
  }
  }
}
