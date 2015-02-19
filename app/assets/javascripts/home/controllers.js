/**
 * Home controllers.
 */
define(['lodash'], function(_) {
  'use strict';

  var HomeCtrl = function($scope, userService) {

    $scope.$watch(function() {
      var user = userService.getUser();
      return user;
    }, function(user) {
      $scope.user = user;
    }, true);

  };

  HomeCtrl.$inject = ['$scope', 'userService'];

  /** Controls the header */
  var HeaderCtrl = function($rootScope, $scope, $location, $mdDialog, userService) {

    // Wrap the current user from the service in a watch expression
    $scope.$watch(function() {
      var user = userService.getUser();
      return user;
    }, function(user) {
      $scope.user = user;
    }, true);

    $scope.tabs = [
      { 'heading': 'Home', url: '/home', user: false, admin: false },
      { 'heading': 'Courses', url: '/courses', user: false, admin: false },
      { 'heading': 'Rounds', url: '/rounds', user: true, admin: false },
      { 'heading': 'Settings', url: '/settings', user: true, admin: false },
      { 'heading': 'Admin', url: '/admin', user: true, admin: true },
      { 'heading': 'Create Course', url: '/courses/new', user: true, admin: true }
    ];

    $scope.$watch('activeTab', function(current, old) {
      if(angular.isDefined(current) && current >= 0 && current !== old) {
        $location.path($scope.tabs[current].url);
      }
    });

    $rootScope.$on('$routeChangeSuccess', function(event, prev, next, current) {

      // Strip out leading '/'
      var nextUrl = $location.url().slice(1);
      var index = -1;

      _.chain($scope.tabs).
        pluck('url').
        map(function(url) { return url.slice(1); }).
        each(function(tabUrl, ix) {
          if(nextUrl === tabUrl) {
            index = ix;
            return;
          }
        }).value();

      $scope.activeTab = index;
    });

    $scope.tabFilter = function(tab) {
      return !tab.user ||
        (!tab.admin && angular.isDefined($scope.user)) ||
        (angular.isDefined($scope.user) && $scope.user.admin);
    };

    $scope.tabClick = function(index) {
      $scope.activeTab = index;
    };

    $scope.showLogin = function(ev) {
      $scope.user = undefined;
      $mdDialog.show({
        templateUrl: '/assets/javascripts/user/views/loginDialog.html',
        onComplete: function() {
          angular.element(document.querySelector('[ng-model="credentials.username"]')).focus();
        },
        targetEvent: ev
      });
    };

    $scope.logout = function() {
      userService.logout();
      $scope.user = undefined;
      $location.path('/home');
    };
  };

  HeaderCtrl.$inject = ['$rootScope', '$scope', '$location', '$mdDialog', 'userService'];

  return {
    HeaderCtrl: HeaderCtrl,
    HomeCtrl: HomeCtrl
  };

});
