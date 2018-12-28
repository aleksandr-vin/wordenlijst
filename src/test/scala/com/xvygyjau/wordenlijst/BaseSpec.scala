package com.xvygyjau.wordenlijst

import github4s.app.GitHub4s
import github4s.free.algebra.{GistOps, UserOps}
import org.scalatest.FlatSpec

trait BaseSpec extends FlatSpec {

  class GistOpsTest extends GistOps[GitHub4s]
  class UserOpsTest extends UserOps[GitHub4s]
  
}
