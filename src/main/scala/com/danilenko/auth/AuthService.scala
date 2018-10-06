package com.danilenko.auth

import com.danilenko.auth.AuthService.Permission.{AdminPermission, Permission, UserPermission}
import com.danilenko.auth.AuthService.UnknownUser

import scala.concurrent.{ExecutionContext, Future}

trait AuthService {
  def login(userName: String,
            password: String): Future[Either[UnknownUser.type, Permission]]
}

class AuthServiceImpl(implicit executionContext: ExecutionContext) extends AuthService {

  override def login(userName: String,
                     password: String): Future[Either[UnknownUser.type, Permission]] =
    Future.successful(
      usersStore
        .find(user => user.userName == userName && user.password == password).map(_.permission)
        .map(Right.apply).getOrElse(Left(UnknownUser))
    )

  // internal
  private lazy val usersStore = Seq(
    User("user", "user", UserPermission),
    User("admin", "admin", AdminPermission)
  )

  private case class User(userName: String, password: String, permission: Permission)
}

object AuthService {

  final object UnknownUser

  object Permission extends Enumeration {
    type Permission = Value
    val UserPermission: Permission = Value("user")
    val AdminPermission: Permission = Value("admin")
  }

}