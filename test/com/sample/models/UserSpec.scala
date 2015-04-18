package com.sample.models

import play.api.Application
import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.DB

import com.sample.models.DAO._

import org.specs2.mutable.Specification

import play.api.test.FakeApplication
import play.api.test.Helpers.{ inMemoryDatabase, running }

class UserSpec extends Specification {

  implicit def app = FakeApplication(additionalConfiguration = inMemoryDatabase())

  def db(implicit app: Application) = DB("test")

  "User" should {

    val T = User

    "None" in {
      val shld = T()
      val tobe = (None, None, None)

      T.unapply(shld) must beSome(tobe)
    }

    "Some" in {
      val shld = T(id = Option(1), email = Option("hoge@sample.com"), name = Option("hoge"))
      val tobe = (Option(1), Option("hoge@sample.com"),
        Option("hoge"))

      T.unapply(shld) must beSome(tobe)
    }
  }

  def sample = db.withSession { implicit s =>
    val (ml, nm) = ("email", "name")
    val id :: _ = samples(ml, nm)
    Users.filter(_.id === id).first
  }

  def samples(email: String = null, name: String = null): Seq[Int] = db.withSession { implicit s =>
    val size = (email, name) match {
      case (null, _) => 10
      case _ => 1
    }
    Users.delete
    (for (i <- (1 to size)) yield {
      val (ml, nm) = (Option(email).getOrElse(s"hoge+${i}@sample.com"), Option(name).getOrElse(s"name-${i}"))
      val u = User(email = Option(ml), name = Option(nm))
      Users.returning(Users.map(_.id)).insert(u)
    }).toList
  }

  "Users" should {

    ".findById" >> {

      def user(id: Int) = db.withSession { implicit s =>
        Users.findById(id)
      }

      "(`id`) must be None/Some" in {
        running(app) {
          val u = sample
          val Some(id) = u.id

          user(id * -1) must beNone
          user(id) must beSome(u)
        }
      }
    }

    ".findByEmail" >> {

      def user(email: String) = db.withSession { implicit s =>
        Users.findByEmail(email)
      }

      "(`email`) must be None/Some" in {
        running(app) {
          val u = sample
          val Some(email) = u.email

          user(s"_${email}") must beNone
          user(email) must beSome(u)
        }
      }
    }

    ".findsByName" >> {

      def users(name: String) = db.withSession { implicit s =>
        Users.filter(_.name === name).list
      }

      "(`name`) must be Nil/Seq" in {
        running(app) {
          val name = "name"
          samples(name = name)

          users(name + "_") must be(Nil)
          users(name) must not be (Nil)
        }
      }
    }
  }
}
