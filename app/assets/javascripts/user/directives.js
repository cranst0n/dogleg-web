define(['angular', './controllers'], function(angular, controllers) {
  'use strict';

  var mod = angular.module('user.directives', []);

  mod.directive('loginDialog', function() {
    return {
      restrict: 'E',
      templateUrl: '/assets/javascripts/user/views/loginDialog.html',
      controller: controllers.LoginCtrl
    };
  });

  mod.directive('changePasswordForm', function() {
    return {
      restrict: 'E',
      templateUrl: '/assets/javascripts/user/views/changePasswordForm.html',
      controller: controllers.ChangePasswordCtrl
    };
  });

  mod.directive('changeAvatarForm', function() {
    return {
      restrict: 'E',
      templateUrl: '/assets/javascripts/user/views/changeAvatarForm.html',
      controller: controllers.ChangeAvatarCtrl
    };
  });

  mod.directive('profileForm', function() {
    return {
      restrict: 'E',
      templateUrl: '/assets/javascripts/user/views/profileForm.html',
      controller: controllers.ProfileFormCtrl
    };
  });

  return mod;
});
