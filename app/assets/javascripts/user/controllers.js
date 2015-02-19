/**
 * User controllers.
 */
define(['common'], function() {
  'use strict';

  var LoginCtrl = function($scope, $location, $mdDialog, userService) {

    $scope.init = function() {
      $scope.credentials = {};
      $scope.loginError = "";
    };

    $scope.resetForm = function() {
      $scope.init();
    };

    $scope.login = function(credentials) {
      userService.loginUser(credentials).then(
        function(user) {
          $scope.init();
          $scope.close();
        },
        function(error) {
          $scope.loginError = error;
          $scope.credentials.password = "";
        });
    };

    $scope.close = function() {
      $mdDialog.hide();
    };

    $scope.init();
  };

  LoginCtrl.$inject = ['$scope', '$location', '$mdDialog', 'userService'];

  var ChangePasswordCtrl = function($scope, $mdToast, userService) {

    $scope.changePassword = function() {
      userService.changePassword(userService.getUser(), $scope.passwordCredentials).
        success(function(response) {
          $mdToast.showSimple('Password changed.');
          $scope.passwordCredentials = {};
        }).error(function(response) {
          $mdToast.showSimple(response.message);
        });
    };
  };

  ChangePasswordCtrl.$inject = ['$scope', '$mdToast', 'userService'];

  var ChangeAvatarCtrl = function($scope, $mdToast, userService, helper) {

    $scope.avatarFile = undefined;

    $scope.$watch('inputFile', function(newFile) {
      if(angular.isDefined(newFile) && newFile[0]) {
        helper.createBase64File(newFile[0]).then(function(fileObject) {
          $scope.avatarFile = fileObject;
        });
      }
    });

    $scope.submitAvatar = function() {
      userService.changeAvatar(userService.getUser(), $scope.avatarFile).then(
        function(response) {
          $mdToast.showSimple('Avatar updated!');
        },
        function(response) {
          $mdToast.showSimple(response.message);
        }
      );
    };

    $scope.onFileChange = function(e, newValue, oldValue) {
    };
  };

  ChangeAvatarCtrl.$inject = ['$scope', '$mdToast', 'userService', 'helper'];

  var ProfileFormCtrl = function($scope, $mdToast, userService, courseService) {

    $scope.$watch(function() {
      var user = userService.getUser();
      return user;
    }, function(user) {
      $scope.user = user;
    }, true);

    $scope.courseSearch = function(text) {
      return courseService.search(text).
        then(function(response) {
          return response.data;
        });
    };

    $scope.courseSelected = function(valueObj) {
      if(angular.isDefined(valueObj.newValue)) {
        courseService.info(valueObj.newValue.id).
          success(function(response) {
            $scope.user.profile.favoriteCourse = response;
          });
      }
    };

    $scope.updateProfile = function(user) {
      $scope.updateProfileError = undefined;
      userService.updateProfile(user).
        success(function(response) {
          $mdToast.showSimple('Profile updated.');
        }).error(function(response) {
          $scope.updateProfileError = response;
        });
    };
  };

  ProfileFormCtrl.$inject = ['$scope', '$mdToast', 'userService', 'courseService'];

  return {
    LoginCtrl: LoginCtrl,
    ChangePasswordCtrl: ChangePasswordCtrl,
    ChangeAvatarCtrl: ChangeAvatarCtrl,
    ProfileFormCtrl: ProfileFormCtrl
  };

});
