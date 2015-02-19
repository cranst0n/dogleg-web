define(['angular', './routes', './controllers', './services', './directives'], function(angular, routes, controllers, services, directives) {
  'use strict';

  var mod = angular.module('dogleg.courses', ['ngRoute', 'common.debounce', 'courses.routes', 'courses.services', 'courses.directives']);

  mod.controller('CourseCatalogCtrl', controllers.CourseCatalogCtrl);
  mod.controller('NewCourseCtrl', controllers.NewCourseCtrl);
  mod.controller('CourseCardCtrl', controllers.CourseCardCtrl);
  mod.controller('RequestCourseCtrl', controllers.RequestCourseCtrl);
  mod.controller('UserRecentCoursesListCtrl', controllers.UserRecentCoursesListCtrl);
  mod.controller('UserRequestedCoursesListCtrl', controllers.UserRequestedCoursesListCtrl);
  mod.controller('ShowCourseCtrl', controllers.ShowCourseCtrl);

  return mod;
});
