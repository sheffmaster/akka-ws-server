package com.danilenko

import com.danilenko.auth.AuthService.Permission.{AdminPermission, UserPermission}
import com.danilenko.auth.AuthService.UnknownUser
import com.danilenko.auth.AuthServiceImpl
import org.scalatest.AsyncFlatSpec

class AuthServiceImplSpec extends AsyncFlatSpec with org.scalatest.concurrent.ScalaFutures with org.scalatest.Matchers {

  "login" should "return admin permissions for login = 'admin' and password = 'admin'" in {
    whenReady(authService.login("admin", "admin")) { result =>
      result shouldBe Right(AdminPermission)
    }
  }

  it should "return user permissions for login = 'user' and password = 'user'" in {
    whenReady(authService.login("user", "user")) { result =>
      result shouldBe Right(UserPermission)
    }
  }

  it should "return UnknownUser permission for unknown login and password combination" in {
    whenReady(authService.login("Sheldon", "Cooper")) { result =>
      result shouldBe Left(UnknownUser)
    }
  }

  // internal

  private val authService = new AuthServiceImpl
}
