package com.sample.models

import scala.slick.lifted.TableQuery

object DAO {

  val Users = new TableQuery(tag => new UserTable(tag)) with UserDao
}
