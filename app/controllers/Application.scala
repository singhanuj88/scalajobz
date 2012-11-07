package controllers

import play.api.mvc.Controller
import play.api._
import play.api.mvc._
import play.api.data.Forms
import play.api.data._
import models.SignUp
import models.SignUpForm
import play.api.mvc.Controller
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.mvc.Http.Request
import play.libs._
import org.bson.types.ObjectId
import models.LogInForm
import models.LogIn
import utils.PasswordHashing
import models.PostAJob
import models.Alert
import models.Employer

object Application extends Controller {

  val signUpForm = Form(
    mapping(
      "EmailId" -> nonEmptyText,
      "Password" -> nonEmptyText,
      "ConfirmPassword" -> nonEmptyText)(SignUpForm.apply)(SignUpForm.unapply))

  /**
   * Login Form Mapping
   */

  val logInForm = Form(
    mapping(
      "EmailId" -> nonEmptyText,
      "Password" -> nonEmptyText)(LogInForm.apply)(LogInForm.unapply))

  def index = Action { implicit request =>
    Ok(views.html.index(new Alert(null, null), request.session.get("userId").getOrElse(null), PostAJob.findAllJobs))
  }

  /**
   * Signup on scalajobz.com
   */

  def signUpOnScalaJobz(flag: String) = Action { implicit request =>
    Ok(views.html.signup(new Alert(null,null),signUpForm, request.session.get("userId").getOrElse(null), flag))
  }

  /**
   * Create A New User
   */
  def newUser(flag: String) = Action { implicit request =>
    signUpForm.bindFromRequest.fold(
      errors => BadRequest(views.html.index(new Alert("error", "There Was Some Errors During The SignUp"), request.session.get("userId").getOrElse(null), PostAJob.findAllJobs)),
      signUpForm => {
        if (!SignUp.findUserByEmail(signUpForm.emailId).isEmpty) {
           Ok(views.html.signup(new Alert("error","This Email Is Already registered With ScalaJobz"),Application.signUpForm, request.session.get("userId").getOrElse(null), flag))
        }
        else {
          val encryptedPassword = (new PasswordHashing).encryptThePassword(signUpForm.password)
          val newUser = Employer(new ObjectId, signUpForm.emailId, encryptedPassword,List(),false)
          val userId = SignUp.createUser(newUser)
          val userSession = request.session + ("userId" -> userId.get.toString)
          if (flag.equals("login"))
            Ok(views.html.index(new Alert("success", "Registration Successful"), userId.get.toString, PostAJob.findAllJobs)).withSession(userSession)
          else
            Ok(views.html.postajob(PostAJobController.postAJobForm, userId.get.toString)).withSession(userSession)

        }
      })
  }

  /**
   * Login On ScalaJobz
   */

  def logIn(flag: String) = Action { implicit request =>
    logInForm.bindFromRequest.fold(
      errors => BadRequest(views.html.index(new Alert("error", "There Was Some Errors During The Login"), null, PostAJob.findAllJobs)),
      logInForm => {
        val encryptedPassword = (new PasswordHashing).encryptThePassword(logInForm.password)
        val users = LogIn.findUser(logInForm.emailId, encryptedPassword)
        println("users" + users.size)
        if (!users.isEmpty) {
          val userSession = request.session + ("userId" -> users(0).id.toString)
          if (flag.equals("login"))
            Ok(views.html.index(new Alert("Success", "Login Successful "), users(0).id.toString, PostAJob.findAllJobs)).withSession(userSession)
          else
            Ok(views.html.postajob(PostAJobController.postAJobForm, users(0).id.toString)).withSession(userSession)
        } else Ok(views.html.login(new Alert("Error", "Invalid Credentials"), Application.logInForm, null, "login"))
      })
  }
  /**
   * Login on scalajobz.com
   */

  def loginOnScalaJobz = Action { implicit request =>
    Ok(views.html.login(new Alert(null, null), logInForm, request.session.get("userId").getOrElse(null), "login"))
  }

  /**
   * Log Out
   */

  def logOutFromScalaJobz = Action {
    Ok(views.html.index(new Alert(null, null), null, PostAJob.findAllJobs)).withNewSession
  }

}