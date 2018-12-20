package com.xvygyjau.wordenlijst

import org.pico.hashids.Hashids

object Ids {
  private lazy val salt = sys.env("HASHIDS_SALT")
  implicit lazy val hashids: Hashids = Hashids.reference(salt)
}
