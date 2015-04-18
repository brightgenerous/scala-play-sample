package com.sample.services

import play.api.Application
import play.api.cache.Cache
import play.api.db.slick.Config.driver.simple._
import play.api.db.slick._
import play.api.libs.json._

import com.sample.models._
import com.sample.models.DAO._

trait UserJsonServiceTrait {

  def get(id: Int)(implicit app: Application, s: Session) =
    getWithCache(id) { id =>
      Users.filter(_.id === id).firstOption.map { user =>
        Json.stringify(Json.obj(
          "data" -> userToJson(user)
        ))
      }
    }

  protected def userToJson(user: User) = Json.obj(
    "id" -> user.id,
    "email" -> user.email,
    "name" -> user.name
  )

  protected def expire = 10

  protected type Ret = Option[String]

  protected def getWithCache(id: Int)(func: Int => Ret)(implicit app: Application): Ret =
    toKey(id) match {
      case Some(key) if !key.isEmpty =>
        Cache.getAs[Ret](key) match {
          case Some(v) => v
          case _ => Symbol(key).synchronized {
            Cache.getOrElse(key, expire) { func(id) }
          }
        }
      case _ => func(id)
    }

  def toKey(id: Int) =
    id match {
      case id if id > 0 => Option(s"cache:user:$id")
      case _ => None
    }
}

object UserJsonService extends UserJsonServiceTrait
