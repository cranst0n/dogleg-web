package controllers

import scaldi.Injector

import play.api._
import play.api.mvc._
import play.api.cache._

class Application(implicit val injector: Injector) extends DoglegController with Security {

  private[this] val routeCacheSeconds = 30

  /** Serves the index page, see views/index.scala.html */
  def index: Action[AnyContent] = Action {
    Ok(views.html.index())
  }

  /**
   * Retrieves all routes via reflection.
   * http://stackoverflow.com/questions/12012703/less-verbose-way-of-generating-play-2s-javascript-router
   * @todo If you have controllers in multiple packages, you need to add each package here.
   */
  val routeCache = {
    val jsRoutesClass = classOf[routes.javascript]
    val controllers = jsRoutesClass.getFields.map(_.get(null))
    controllers.flatMap { controller =>
      controller.getClass.getDeclaredMethods.map { action =>
        action.invoke(controller).asInstanceOf[play.core.Router.JavascriptReverseRoute]
      }
    }
  }

  /**
   * Returns the JavaScript router that the client can use for "type-safe" routes.
   * Uses browser caching; set duration (in seconds) according to your release cycle.
   * @param varName The name of the global variable, defaults to `jsRoutes`
   */
  def jsRoutes(varName: String = "jsRoutes"): EssentialAction = Cached(_ => "jsRoutes", duration = routeCacheSeconds) {
    Action { implicit request =>
      Ok(Routes.javascriptRouter(varName)(routeCache: _*)).as(JAVASCRIPT)
    }
  }
}
