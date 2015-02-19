
import play.api.GlobalSettings
import play.api.mvc.WithFilters
import play.filters.gzip.GzipFilter

import scaldi.play.ScaldiSupport

import services._
import services.slick._

object Global extends WithFilters(new GzipFilter()) with GlobalSettings
  with ScaldiSupport {

  def applicationModule = ProductionModule() :: WebModule()
}
