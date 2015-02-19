/**
 * Configure routes of user module.
 */
define(['angular', './controllers'], function(angular, controllers) {
  'use strict';

  var mod = angular.module('user.routes', ['user.services']);

  mod.config(['$routeProvider', 'userResolve', function($routeProvider,userResolve) {
    $routeProvider
      .when('/settings', {
        templateUrl:'/assets/javascripts/user/views/settings.html',
        resolve: userResolve
      });
  }]);

  return mod;
});
