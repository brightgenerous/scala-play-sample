package controllers

import play.api.{ Application => App }

import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.DB
import play.api.mvc.Call

import com.sample.models._
import com.sample.models.DAO._

import org.specs2.mutable.Specification

import play.api.test._
import play.api.test.Helpers._

class UsersControllerSpec extends Specification {

  def app =
    FakeApplication(additionalConfiguration = inMemoryDatabase("test"))

  def db(implicit app: App) = DB("test")

  def insertUser(email: String, name: String)(implicit app: App) =
    db.withSession { implicit s =>
      Users.insert(User(email = Option(email), name = Option(name)))
    }

  def userById(id: Int)(implicit app: App) =
    db.withSession { implicit s =>
      Users.findById(id)
    }

  def userByEmail(email: String)(implicit app: App) =
    db.withSession { implicit s =>
      Users.findByEmail(email)
    }

  "UsersController" should {

    "#index" >> {

      def call = controllers.api.routes.UsersController.index

      "url" in {
        val Call(method, url) = call

        method must be_==("GET")
        url must be_==("/users")
      }

      "success" in {

        val (email, name) = ("hoge@example.com", "hoge")

        implicit val _app = app
        running(_app) {
          insertUser(email, name)
          val Some(user) = userByEmail(email)
          val Some(id) = user.id

          val Call(method, url) = call
          val request = FakeRequest(method, url)

          val res = route(request).get

          status(res) must be_==(OK)
          contentType(res) must beSome("application/json")
          val content = contentAsString(res)
          content must contain(s"""{"data":[{""")
          content must contain(s""""id":${id}""")
          content must contain(s""""name":"${name}"""")
        }
      }

      "nothing" in {

        implicit val _app = app
        running(_app) {
          val Call(method, url) = call
          val request = FakeRequest(method, url)

          val res = route(request).get

          status(res) must be_==(OK)
          contentType(res) must beSome("application/json")
          val content = contentAsString(res)
          content must contain(s"""{"data":[]}""")
        }
      }
    }

    "#show" >> {

      def call(id: Int) = controllers.api.routes.UsersController.show(id)

      "url" in {
        val id = 1
        val Call(method, url) = call(id)

        method must be_==("GET")
        url must be_==(s"/users/${id}")
      }

      "success" in {

        val (email, name) = ("hoge@example.com", "hoge")

        implicit val _app = app
        running(_app) {
          insertUser(email, name)
          val Some(user) = userByEmail(email)
          val Some(id) = user.id

          val Call(method, url) = call(id)
          val request = FakeRequest(method, url)

          val res = route(request).get

          status(res) must be_==(OK)
          contentType(res) must beSome("application/json")
          val content = contentAsString(res)
          content must contain(s"""{"data":{""")
          content must contain(s""""id":${id}""")
          content must contain(s""""name":"${name}"""")
        }
      }

      "nothing" in {

        implicit val _app = app
        running(_app) {
          val id = 1

          val Call(method, url) = call(id)
          val request = FakeRequest(method, url)

          val res = route(request).get

          status(res) must be_==(BAD_REQUEST)
          contentType(res) must beSome("application/json")
        }
      }
    }

    "#create" >> {

      def call = controllers.api.routes.UsersController.create

      "url" in {
        val Call(method, url) = call

        method must be_==("POST")
        url must be_==("/users")
      }

      "success" in {

        examplesBlock {

          val sets = Seq(
            ("hoge@example.com", "hoge")
          )

          for ((ml, nm) <- sets) {

            val params = Map("email" -> ml, "name" -> nm)
              .filter(t => t._2 != null)
            val Call(method, url) = call
            val request = FakeRequest(method, url)
              .withFormUrlEncodedBody(params.toSeq: _*)

            implicit val _app = app
            running(_app) {
              val res = route(request).get

              status(res) must be_==(OK)
              contentType(res) must beSome("application/json")

              val Some(u) = userByEmail(ml)
              u.email must beSome(ml)
              u.name must beSome(nm)
            }
          }
        }
      }

      "invalid" in {

        examplesBlock {

          val sets = Seq(
            ((null, ""), "error.required"),
            (("", ""), "error.minLength")
          )

          for (((ml, nm), msg) <- sets) {

            val params = Map("email" -> ml, "name" -> nm)
              .filter(t => t._2 != null)
            val Call(method, url) = call
            val request = FakeRequest(method, url)
              .withFormUrlEncodedBody(params.toSeq: _*)

            implicit val _app = app
            running(_app) {
              val res = route(request).get

              status(res) must be_==(BAD_REQUEST)
              contentType(res) must beSome("application/json")
              contentAsString(res) must contain(msg)

              userByEmail(ml) must beNone
            }
          }
        }
      }

      "duplicate" in {

        val (email, name) = ("hoge@example.com", "hoge")
        val (ml, nm) = (email, "hoge+1")

        implicit val _app = app
        running(_app) {
          insertUser(email, name)
          val Some(id) = userByEmail(email).flatMap(_.id)

          val params = Map("email" -> ml, "name" -> nm)
            .filter(t => t._2 != null)
          val Call(method, url) = call
          val request = FakeRequest(method, url)
            .withFormUrlEncodedBody(params.toSeq: _*)

          val res = route(request).get

          status(res) must be_==(BAD_REQUEST)
          contentType(res) must beSome("application/json")

          val Some(u) = userByEmail(ml)
          u.email must beSome(email)
          u.name must beSome(name)
        }
      }
    }

    "#update" >> {

      def call(id: Int) = controllers.api.routes.UsersController.update(id)

      "url" in {
        val id = 1
        val Call(method, url) = call(id)

        method must be_==("PUT")
        url must be_==(s"/users/${id}")
      }

      "success" in {

        val (email, name) = ("hoge@example.com", "hoge")
        val (ml, nm) = ("hoge+1@example.com", "hoge+1")

        implicit val _app = app
        running(_app) {
          insertUser(email, name)
          val Some(id) = userByEmail(email).flatMap(_.id)

          val params = Map("email" -> ml, "name" -> nm)
            .filter(t => t._2 != null)
          val Call(method, url) = call(id)
          val request = FakeRequest(method, url)
            .withFormUrlEncodedBody(params.toSeq: _*)

          val res = route(request).get

          status(res) must be_==(OK)
          contentType(res) must beSome("application/json")

          val Some(u) = userById(id)
          u.email must beSome(ml)
          u.name must beSome(nm)
        }
      }

      "nothing" in {

        val (ml, nm) = ("hoge+1@example.com", "hoge+1")

        implicit val _app = app
        running(_app) {
          val id = 1

          val params = Map("email" -> ml, "name" -> nm)
            .filter(t => t._2 != null)
          val Call(method, url) = call(id)
          val request = FakeRequest(method, url)
            .withFormUrlEncodedBody(params.toSeq: _*)

          val res = route(request).get

          status(res) must be_==(BAD_REQUEST)
          contentType(res) must beSome("application/json")
        }
      }

      "invalid" in {

        val (email, name) = ("hoge@example.com", "hoge")
        val ((ml, nm), msg) = (("", ""), "error.minLength")

        implicit val _app = app
        running(_app) {
          insertUser(email, name)
          val Some(id) = userByEmail(email).flatMap(_.id)

          val params = Map("email" -> ml, "name" -> nm)
            .filter(t => t._2 != null)
          val Call(method, url) = call(id)
          val request = FakeRequest(method, url)
            .withFormUrlEncodedBody(params.toSeq: _*)

          val res = route(request).get

          status(res) must be_==(BAD_REQUEST)
          contentType(res) must beSome("application/json")
          contentAsString(res) must contain(msg)

          val Some(u) = userById(id)
          u.email must beSome(email)
          u.name must beSome(name)
        }
      }

      "duplicate" in {

        val (email0, name0) = ("hoge+1@example.com", "hoge+1")
        val (email, name) = ("hoge@example.com", "hoge")
        val (ml, nm) = (email0, "hoge+2")

        implicit val _app = app
        running(_app) {
          insertUser(email0, name0)
          insertUser(email, name)
          val Some(id) = userByEmail(email).flatMap(_.id)

          val params = Map("email" -> ml, "name" -> nm)
            .filter(t => t._2 != null)
          val Call(method, url) = call(id)
          val request = FakeRequest(method, url)
            .withFormUrlEncodedBody(params.toSeq: _*)

          val res = route(request).get

          status(res) must be_==(BAD_REQUEST)
          contentType(res) must beSome("application/json")

          val Some(u) = userByEmail(ml)
          u.email must beSome(email0)
          u.name must beSome(name0)
        }
      }
    }

    "#delete" >> {

      def call(id: Int) = controllers.api.routes.UsersController.delete(id)

      "url" in {
        val id = 1
        val Call(method, url) = call(id)

        method must be_==("DELETE")
        url must be_==(s"/users/${id}")
      }

      "success" in {

        val (email, name) = ("hoge@example.com", "hoge")

        implicit val _app = app
        running(_app) {
          insertUser(email, name)
          val Some(id) = userByEmail(email).flatMap(_.id)

          val Call(method, url) = call(id)
          val request = FakeRequest(method, url)

          val res = route(request).get

          status(res) must be_==(OK)
          contentType(res) must beSome("application/json")

          userById(id) must beNone
        }
      }

      "nothing" in {

        implicit val _app = app
        running(_app) {
          val id = 1

          val Call(method, url) = call(id)
          val request = FakeRequest(method, url)

          val res = route(request).get

          status(res) must be_==(BAD_REQUEST)
          contentType(res) must beSome("application/json")
        }
      }
    }
  }
}
