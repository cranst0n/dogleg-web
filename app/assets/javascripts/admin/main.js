/**
 * Main, shows the start page and provides controllers for the header and the footer.
 * This the entry module which serves as an entry point so other modules only have to include a
 * single module.
 */
define(['angular', 'angular-route', './routes', './controllers', './directives'], function(angular, ngRoute, routes, controllers, directives) {
  'use strict';

  var mod = angular.module('dogleg.admin', ['ngRoute', 'admin.routes', 'admin.directives']);

  mod.controller('AdminCtrl', controllers.AdminCtrl);
  mod.controller('UnapprovedListCtrl', controllers.UnapprovedListCtrl);
  mod.controller('RequestedCourseListCtrl', controllers.RequestedCourseListCtrl);
  mod.controller('QuickCourseImportCtrl', controllers.QuickCourseImportCtrl);

  return mod;
});
