/**
 * Home routes.
 */
define(['angular', './controllers'], function(angular, controllers) {
  'use strict';

  var mod = angular.module('admin.routes', ['user.services']);

  mod.config(['$routeProvider', 'adminResolve', function($routeProvider, adminResolve) {
    $routeProvider
      .when('/admin', {
        templateUrl: '/assets/javascripts/admin/views/admin.html',
        resolve: adminResolve
      });
  }]);

  return mod;
});
