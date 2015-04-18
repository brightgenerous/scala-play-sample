package controllers.api

import play.api.Play.current
import play.api.data.Form
import play.api.data.Forms._
import play.api.db.slick.Config.driver.simple._
import play.api.db.slick._
import play.api.libs.json._
import play.api.mvc._

import com.sample.models._
import com.sample.models.DAO._

trait UsersControllerTrait {

  self: Controller =>

  private object UserForm {

    private val EMAIL = "email" -> text(minLength = 5, maxLength = 255)
    private val NAME = "name" -> text(minLength = 3, maxLength = 20)

    val create = Form(mapping(
      EMAIL, NAME
    )((email, name) =>
        User(email = Option(email), name = Option(name))
      )((user: User) =>
        Option(user.email.orNull, user.name.orNull)
      ))

    val update = Form(mapping(
      EMAIL, NAME
    )((email, name) =>
        User(email = Option(email), name = Option(name))
      )((user: User) =>
        user.id match {
          case Some(id) =>
            Option((user.email.orNull, user.name.orNull))
          case _ => None
        }
      ))
  }

  private def validateError(form: Form[_]) =
    BadRequest(Json.toJson(Map("errors" -> form.errors.map(_.message))))

  private def userToJson(user: User) = Json.obj(
    "id" -> user.id,
    "email" -> user.email,
    "name" -> user.name
  )

  private def usersToJson(users: Seq[User]) =
    Json.toJson(users.map(userToJson))

  def index = DBAction { implicit s =>
    val json = Json.obj(
      "data" -> usersToJson(Users.list)
    )
    Ok(json).as("application/json")
  }

  def show(id: Int) = DBAction { implicit s =>
    val res = Users.filter(_.id === id).firstOption match {
      case Some(user) => {
        val json = Json.obj(
          "data" -> userToJson(user)
        )
        Ok(json)
      }
      case _ => BadRequest
    }
    res.as("application/json")
  }

  def create = DBAction { implicit s =>
    UserForm.create.bindFromRequest.fold(
      form => validateError(form),
      user => {
        Users.filter(_.email === user.email).exists.run match {
          case true => BadRequest
          case _ => {
            Users.insert(user)
            Ok
          }
        }
      }).as("application/json")
  }

  def update(id: Int) = DBAction { implicit s =>
    UserForm.update.bindFromRequest.fold(
      form => validateError(form),
      user => {
        Users.filterNot(_.id === id).filter(_.email === user.email).exists.run match {
          case true => BadRequest
          case _ => {
            val count = Users.filter(_.id === id).map(v => (v.email, v.name))
              .update((user.email.orNull, user.name.orNull))
            count match {
              case 0 => BadRequest
              case _ => Ok
            }
          }
        }
      }).as("application/json")
  }

  def delete(id: Int) = DBAction { implicit s =>
    val count = Users.filter(_.id === id).delete
    val res = count match {
      case 0 => BadRequest
      case _ => Ok
    }
    res.as("application/json")
  }
}

object UsersController extends Controller with UsersControllerTrait
