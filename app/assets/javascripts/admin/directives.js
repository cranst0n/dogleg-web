define(['angular', './controllers'], function(angular, controllers) {
  'use strict';

  var mod = angular.module('admin.directives', []);

  mod.directive('unapprovedCourseList', function() {
    return {
      restrict: 'E',
      templateUrl: '/assets/javascripts/admin/views/unapprovedCourseList.html',
      controller: controllers.UnapprovedListCtrl
    };
  });

  mod.directive('requestedCourseList', function() {
    return {
      restrict: 'E',
      templateUrl: '/assets/javascripts/admin/views/requestedCourseList.html',
      controller: controllers.RequestedCourseListCtrl
    };
  });

  mod.directive('quickCourseImport', function() {
    return {
      restrict: 'E',
      templateUrl: '/assets/javascripts/admin/views/quickCourseImport.html',
      controller: controllers.QuickCourseImportCtrl
    };
  });

  return mod;
});
