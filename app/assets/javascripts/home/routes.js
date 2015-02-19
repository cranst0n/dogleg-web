/**
 * Home routes.
 */
define(['angular', './controllers'], function(angular, controllers) {
  'use strict';

  var mod = angular.module('home.routes', []);

  mod.config(['$routeProvider', function($routeProvider) {
    $routeProvider
      .when('/home', {
        templateUrl: '/assets/javascripts/home/views/home.html',
        controller: controllers.HomeCtrl
      }).otherwise({
        templateUrl: '/assets/javascripts/home/views/notFound.html'
      });
  }]);

  return mod;
});
