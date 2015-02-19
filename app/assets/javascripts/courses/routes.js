define(['angular', './controllers'], function(angular, controllers) {
  'use strict';

  var mod = angular.module('courses.routes', []);

  mod.config(['$routeProvider', function($routeProvider) {
    $routeProvider
      .when('/courses', {
        templateUrl: '/assets/javascripts/courses/views/catalog.html',
        controller: controllers.CourseCatalogCtrl
      }).
      when('/courses/new', {
        templateUrl: '/assets/javascripts/courses/views/new.html',
        controller: controllers.NewCourseCtrl
      }).
      when('/courses/request', {
        templateUrl: '/assets/javascripts/courses/views/request.html',
        controller: controllers.RequestCourseCtrl
      }).
      when('/courses/show/:courseId', {
        templateUrl: '/assets/javascripts/courses/views/show.html',
        controller: controllers.ShowCourseCtrl
      });
  }]);

  return mod;
});
