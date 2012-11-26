package controllers
import play.api.mvc.Controller
import play.api._
import play.api.mvc._
import play.api.data.Forms
import play.api.data._
import play.api.mvc.Controller
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.mvc.Http.Request
import play.libs._
import org.bson.types.ObjectId
import models.PostAJobForm
import models.JobEntity
import models.Job
import java.util.Date
import models.Alert
import models.Common

object PostAJobController extends Controller {

  val postAJobForm = Form(
    mapping(
      "Position" -> nonEmptyText,
      "Company" -> nonEmptyText,
      "Location" -> nonEmptyText,
      "JobType" -> nonEmptyText,
      "Email_Addrss_To_Apply_To" -> nonEmptyText,
      "Skills" -> nonEmptyText,
      "Description" -> nonEmptyText)(PostAJobForm.apply)(PostAJobForm.unapply))

  /**
   * Load  Job  Page on scalajobz.com
   */

  def postAJob = Action { implicit request =>
    if (request.session.get("userId") == None)
      Ok(views.html.login(new Alert(null, null), Application.logInForm, request.session.get("userId").getOrElse(null), "jobPost"))
    else
      Ok(views.html.postajob(postAJobForm, request.session.get("userId").getOrElse(null)))
  }

  /**
   * Post A Job on scalajobz.com
   */
  def newJob = Action { implicit request =>
    postAJobForm.bindFromRequest.fold(
      errors => BadRequest(views.html.index(new Alert("error", "There Was Some Errors During Job Posting"), request.session.get("userId").getOrElse(null), Job.findAllJobs, false)),
      postAJobForm => {
        if (postAJobForm.position == "" || postAJobForm.company == "" || postAJobForm.location == ""
          || postAJobForm.jobType == "" || postAJobForm.jobType.equals("-- Select Job Type --") || postAJobForm.emailAddress == "") Ok("Please Fill The Mendatory Fields")
        else {
          if (request.session.get("userId") == None) Ok(views.html.login(new Alert(null, null), Application.logInForm, request.session.get("userId").getOrElse(null), "jobPost"))
          else {
            val job = JobEntity(new ObjectId, new ObjectId(request.session.get("userId").get), postAJobForm.position, postAJobForm.company, postAJobForm.location, postAJobForm.jobType, postAJobForm.emailAddress, postAJobForm.skillsRequired.split(",").toList, postAJobForm.description, new Date)
            Job.addJob(job)
            val jobPostByUserList = Job.findJobsPostByUserId(new ObjectId(request.session.get("userId").get))
            //Ok(views.html.index(new Alert("success", "Job Posted Successfully"), request.session.get("userId").getOrElse(null), jobPostByUserList, true))
            //Results.Redirect("/findJobPostByUserId?alert=success&message=Job Posted Successfully")
            Common.setAlert(new Alert("success","Job Posted Successfully"))
            Results.Redirect("/findJobPostByUserId")
          }
        }
      })
  }

  /**
   * Load  Job  Page on scalajobz.com
   */

  def findAJob(searchString: String, editFlag: String) = Action { implicit request =>
    val searchJobList = Job.searchTheJob(searchString)
    if (editFlag.equals("true"))
      Ok(views.html.ajax_result(searchJobList, true))
    else
      Ok(views.html.ajax_result(searchJobList, false))
  }

  def findJobDetail(jobId: String) = Action { implicit request =>
    val job: Option[JobEntity] = Job.findJobDetail(new ObjectId(jobId))
    Ok(views.html.jobDetail(job.get, request.session.get("userId").getOrElse(null)))
  }

  def findJobPostForEdit(jobId: String) = Action { implicit request =>
    val job = Job.findJobDetail(new ObjectId(jobId)).get
    Ok(views.html.editJob(job, postAJobForm, request.session.get("userId").getOrElse(null)))
  }

  /**
   * Edit Job
   */
  def editJob(jobId: String) = Action { implicit request =>
    val existJob = Job.findJobDetail(new ObjectId(jobId))
    existJob match {
      case None => Results.Redirect(routes.UserController.findJobPostByUserId)
      case Some(job: JobEntity) =>
        postAJobForm.bindFromRequest.fold(
          errors => BadRequest(views.html.editJob(job, postAJobForm, request.session.get("userId").getOrElse(null))),
          postAJobForm => {
            val editJob = JobEntity(job.id, job.userId, postAJobForm.position, postAJobForm.company, postAJobForm.location, postAJobForm.jobType, postAJobForm.emailAddress, postAJobForm.skillsRequired.split(",").toList, postAJobForm.description, new Date)
            Job.updateJob(editJob)
            val jobPostByUserList = Job.findJobsPostByUserId(new ObjectId(request.session.get("userId").get))
            Ok(views.html.index(new Alert("success", "Job Updated Successfully"), request.session.get("userId").getOrElse(null), jobPostByUserList, true))

          })
    }
  }

  /*
   * Delete Job
   * */

  def deleteJob(jobId: String, editFlag: String) = Action { implicit request =>
    Job.deleteJobByJobId(new ObjectId(jobId))
    val jobPostByUserList = Job.findJobsPostByUserId(new ObjectId(request.session.get("userId").get))
    if (editFlag.equals("true"))
      Ok(views.html.ajax_result(jobPostByUserList, true))
    else
      Ok(views.html.ajax_result(jobPostByUserList, false))
  }

}