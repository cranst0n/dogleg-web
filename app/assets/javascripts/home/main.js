/**
 * Main, shows the start page and provides controllers for the header and the footer.
 * This the entry module which serves as an entry point so other modules only have to include a
 * single module.
 */
define(['angular', 'angular-route', './routes', './controllers', './directives'], function(angular, ngRoute, routes, controllers, directives) {
  'use strict';

  var mod = angular.module('dogleg.home', ['ngRoute', 'home.routes', 'home.directives']);

  mod.controller('HeaderCtrl', controllers.HeaderCtrl);
  mod.controller('HomeCtrl', controllers.HomeCtrl);

  return mod;
});
