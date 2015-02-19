define(['angular', './controllers'], function(angular, controllers) {
  'use strict';

  var mod = angular.module('courses.directives', []);

  mod.directive('courseCard', function() {
    return {
      restrict: 'E',
      templateUrl: '/assets/javascripts/courses/views/card.html',
      controller: controllers.CourseCardCtrl,
      scope: {
        'course': '=',
        'readOnly': '='
      }
    };
  });

  mod.directive('userRecentCourseList', function() {
    return {
      restrict: 'E',
      templateUrl: '/assets/javascripts/courses/views/userRecentCourseList.html',
      controller: controllers.UserRecentCoursesListCtrl
    };
  });

  mod.directive('userRequestedCourseList', function() {
    return {
      restrict: 'E',
      templateUrl: '/assets/javascripts/courses/views/userRequestedCourseList.html',
      controller: controllers.UserRequestedCoursesListCtrl
    };
  });

  return mod;
});
