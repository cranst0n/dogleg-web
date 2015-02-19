/**
 * User package module. Manages all sub-modules so other RequireJS modules
 * only have to import the package.
 */
define(['angular', './routes', './services', './controllers', './directives'], function(angular, routes, services, controllers, directives) {
  'use strict';

  var mod = angular.module('dogleg.user',
    ['ngCookies', 'ngRoute', 'user.routes', 'user.services', 'user.directives']);

  mod.controller('LoginCtrl', controllers.LoginCtrl);
  mod.controller('ChangePasswordCtrl', controllers.ChangePasswordCtrl);
  mod.controller('ChangeAvatarCtrl', controllers.ChangeAvatarCtrl);
  mod.controller('ProfileFormCtrl', controllers.ProfileFormCtrl);

  return mod;
});
