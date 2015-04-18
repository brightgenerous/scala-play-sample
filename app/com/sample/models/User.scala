package com.sample.models

import play.api.db.slick.Config.driver.simple._

import scala.slick.lifted.{ Ordered, TableQuery }

case class User(id: Option[Int] = None, email: Option[String] = None, name: Option[String] = None)

class UserTable(tag: Tag) extends Table[User](tag, "users") {

  val id = column[Int]("id", O.AutoInc, O.PrimaryKey)
  val email = column[String]("email")
  val name = column[String]("name")

  def * = (id.?, email.?, name.?) <> (User.tupled, User.unapply)

  lazy val unique1 = index("idx_users_unq_1", email, unique = true)
}

trait UserDao {

  self: TableQuery[UserTable] =>

  def findById(id: Int)(implicit s: Session): Option[User] =
    filter(_.id === id).firstOption

  def findByEmail(email: String)(implicit s: Session): Option[User] =
    filter(_.email === email).firstOption
}
