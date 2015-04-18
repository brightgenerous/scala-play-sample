package com.sample.services

import play.api.{ Application => App }

import play.api.cache.Cache

import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.DB

import com.sample.models._
import com.sample.models.DAO._

import org.specs2.mutable.Specification

import play.api.test._
import play.api.test.Helpers._

class UserJsonServiceSpec extends Specification {

  object Brief extends UserJsonServiceTrait {

    override def expire = 1 // seconds
  }

  object Longevity extends UserJsonServiceTrait {

    override def expire = 60 * 60
  }

  def app =
    FakeApplication(additionalConfiguration = inMemoryDatabase("test"))

  def db(implicit app: App) = DB("test")

  def insertUser(email: String, name: String)(implicit app: App) =
    db.withSession { implicit s =>
      Users.insert(User(email = Option(email), name = Option(name)))
    }

  def deleteUser(id: Int)(implicit app: App) =
    db.withSession { implicit s =>
      Users.filter(_.id === id).delete
    }

  def userByEmail(email: String)(implicit app: App) =
    db.withSession { implicit s =>
      Users.findByEmail(email)
    }

  "UserJsonService" should {

    "#get" >> {

      "exists in cache" in {

        val Service = Longevity

        val (email, name) = ("hoge@example.com", "hoge")

        implicit val _app = app
        running(_app) {
          insertUser(email, name)
          val Some(user) = userByEmail(email)
          val Some(id) = user.id

          val _ = db.withSession { implicit s =>
            Service.get(id)
          }

          deleteUser(id)

          val userOpt = db.withSession { implicit s =>
            Service.get(id)
          }

          userOpt must beSome
        }
      }

      "not exists in cache" in {

        val Service = Longevity

        val (email, name) = ("hoge+1@example.com", "hoge")

        implicit val _app = app
        running(_app) {
          insertUser(email, name)
          val Some(user) = userByEmail(email)
          val Some(id) = user.id

          val _ = db.withSession { implicit s =>
            Service.get(id)
          }

          deleteUser(id)
          Cache.remove(Service.toKey(id).get)

          val userOpt = db.withSession { implicit s =>
            Service.get(id)
          }

          userOpt must beNone
        }
      }

      "expire cache" in {

        val Service = Brief

        val (email, name) = ("hoge+2@example.com", "hoge")

        implicit val _app = app
        running(_app) {
          insertUser(email, name)
          val Some(user) = userByEmail(email)
          val Some(id) = user.id

          val _ = db.withSession { implicit s =>
            Service.get(id)
          }

          deleteUser(id)
          Thread.sleep(2000) // milliseconds

          val userOpt = db.withSession { implicit s =>
            Service.get(id)
          }

          userOpt must beNone
        }
      }
    }
  }
}
