package com.xvygyjau.wordenlijst.testdata

import com.xvygyjau.wordenlijst.github.AccessToken

package object valid {
  val hashidsSalt = "test"
  val accessToken = AccessToken("cb213d0c3c98e33730862234d414c040d1c188df")
  val apiKey = "l80M3SMbNPJXow9tQVwl8jRq3FX76MnZNb4"

  val newPhrase = "bebop scale"

  object Gist {
    val url = "https://some-gist-url"
    val id = "123456"
    val description = "Wordenlijst"
    val public = true
    val filename = "wordenlijst"
    val fileContent = "+++\n"
  }

  object User {
    val id = 654321
    val login = "bobik"
    val avatarUrl = "https://some-avatar-url"
    val htmlUrl = "https://some-html-url"
    val name = Option("Bobik, who loves tests")
  }
}
