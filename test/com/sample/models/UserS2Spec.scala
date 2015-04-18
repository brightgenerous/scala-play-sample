package com.sample.models

import play.api.Application
import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.DB

import com.sample.models.DAO._

import org.specs2.execute.AsResult
import org.specs2.specification.{ AroundExample, Grouped }
import org.specs2.specification.script.Specification

import play.api.test.FakeApplication
import play.api.test.Helpers.{ inMemoryDatabase, running }

class UserS2Spec extends Specification with AroundExample with Grouped {

  override def is = s2"""
DAO.Users
============
◆ DAO.Usersオブジェクトのテスト
  ◇ Userが1つも登録されていないとき
    + Userリストは空である
    Userを1つ登録すると
    + User数は1であり
    + 追加した値を持つUserがUserリストに含まれている
  ◇ Userが1つ登録されているとき
    先頭のUserを更新すると
    + User数は1であり
    + 更新した値を持つUserがUserリストに含まれている
    先頭のUserを削除すると
    + User数は0であり
    + Userリストは空である
"""

  "DAO.Users" - new group {

    eg := Empty.actual must be(Nil)

    eg := Create.actual must have size 1
    eg := Create.actual.map(v => (v.email.orNull, v.name.orNull)) must have contain Create.values

    eg := Update.actual must have size 1
    eg := Update.actual.map(v => (v.email.orNull, v.name.orNull)) must have contain Update.values

    eg := Delete.actual must have size 0
    eg := Delete.actual must be(Nil)
  }

  implicit def app = FakeApplication(additionalConfiguration = inMemoryDatabase())

  def db(implicit app: Application) = DB("test")

  private[this] var first = true

  override def around[T: AsResult](t: => T) = running(app) {
    if (first) {
      first = false
      db.withSession { implicit s =>
        Users.delete
      }
    }
    AsResult.effectively(t)
  }

  object Empty {

    lazy val actual =
      db.withSession { implicit s =>
        Users.list
      }
  }

  object Create {

    val values = ("hoge@sample.com", "name")

    lazy val actual =
      db.withSession { implicit s =>
        Users.insert(User(email = Option(values._1), name = Option(values._2)))
        // otherwise
        // Users.map(v => (v.email, v.name)) += (values)
        Users.list
      }
  }

  object Update {

    val values = ("hoge+1@sample.com", "name+1")

    lazy val actual =
      db.withSession { implicit s =>
        Users.firstOption
          .foreach { u =>
            Users.filter(_.id === u.id).map(v => (v.email, v.name)).update(values)
          }
        Users.list
      }
  }

  object Delete {

    lazy val actual =
      db.withSession { implicit s =>
        Users.firstOption
          .foreach { u =>
            Users.filter(_.id === u.id).delete
          }
        Users.list
      }
  }
}
