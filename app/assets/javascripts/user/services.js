/**
 * User service, exposes user model to the rest of the app.
 */
 /* global CryptoJS */
define(['angular', 'common', 'CryptoJS'], function (angular) {
  'use strict';

  var mod = angular.module('user.services', ['dogleg.common', 'ngCookies']);

  mod.factory('userService', ['$http', '$q', 'playRoutes', '$cookies', '$log', function ($http, $q, playRoutes, $cookies, $log) {

    var user, token = $cookies.get('XSRF-TOKEN');

    /* If the token is assigned, check that the token is still valid on the server */
    if(angular.isDefined(token)) {

      $log.debug('Restoring user from cookie...');

      playRoutes.controllers.Authentication.authUser().get()
        .success(function (data) {
          $log.debug('Welcome back, ' + data.name);
          user = data;
        }).error(function () {
          $log.debug('Token no longer valid, please log in.');
          token = undefined;
          delete $cookies.remove('XSRF-TOKEN');
          return $q.reject("Token invalid");
        });
    }

    return {
      loginUser: function (credentials) {

        var hashedCredentials = {
          username: credentials.username,
          password: CryptoJS.SHA3(credentials.password).toString()
        };

        return playRoutes.controllers.Authentication.login().post(hashedCredentials).then(function (response) {
          // return promise so we can chain easily
          token = response.data.token;
          return playRoutes.controllers.Authentication.authUser().get();
        }).then(function (response) {
          user = response.data;
          return user;
        });
      },
      logout: function () {
        // Logout on server in a real app
        delete $cookies.remove('XSRF-TOKEN');
        token = undefined;
        user = undefined;
        return playRoutes.controllers.Authentication.logout().post().then(function () {
          $log.debug("Good bye");
        });
      },
      getUser: function () {
        return user;
      },
      updateProfile: function(user) {
        return playRoutes.controllers.Users.updateProfile(user.id).put(user.profile);
      },
      changePassword: function(user, credentials) {
        var hashedCredentials = {
          oldPassword: CryptoJS.SHA3(credentials.oldPassword).toString(),
          newPassword: CryptoJS.SHA3(credentials.newPassword).toString(),
          newPasswordConfirm: CryptoJS.SHA3(credentials.newPasswordConfirm).toString()
        };

        return playRoutes.controllers.Users.changePassword(user.id).put(hashedCredentials);
      },
      changeAvatar: function(user, avatarFile) {
        return playRoutes.controllers.Users.changeAvatar(user.id).put(avatarFile).then(function(response) {
          user = response.data;
          return response;
        });
      }
    };
  }]);

  /**
   * Add this object to a route definition to only allow resolving the route if the user is
   * logged in. This also adds the contents of the objects as a dependency of the controller.
   */
  mod.constant('userResolve', {
    user: ['$q', 'userService', function ($q, userService) {
      var deferred = $q.defer();
      var user = userService.getUser();
      if(angular.isDefined(user)) {
        deferred.resolve(user);
      } else {
        deferred.reject();
      }
      return deferred.promise;
    }]
  });

  mod.constant('adminResolve', {
    user: ['$q', 'userService', function ($q, userService) {
      var deferred = $q.defer();
      var user = userService.getUser();
      if(angular.isDefined(user) && user.admin) {
        deferred.resolve(user);
      } else {
        deferred.reject();
      }
      return deferred.promise;
    }]
  });

  /**
   * If the current route does not resolve, go back to the start page.
   */
  var handleRouteError = function ($rootScope, $location) {
    $rootScope.$on('$routeChangeError', function (e, next, current) {
      $location.path('/home');
    });
  };

  handleRouteError.$inject = ['$rootScope', '$location'];
  mod.run(handleRouteError);

  return mod;
});
