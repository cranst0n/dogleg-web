define(['angular', './controllers', './services'], function(angular, controllers, services) {
  'use strict';

  var mod = angular.module('rounds.routes', ['rounds.services']);

  mod.config(['$routeProvider', 'userResolve', function($routeProvider, userResolve) {
    $routeProvider
      .when('/rounds', {
        templateUrl: '/assets/javascripts/rounds/views/catalog.html',
        controller: controllers.RoundCatalogCtrl,
        resolve: userResolve
      }).when('/rounds/new', {
        templateUrl: '/assets/javascripts/rounds/views/new.html',
        controller: controllers.NewRoundCtrl,
        resolve: userResolve
      }).when('/rounds/show/:roundId', {
        templateUrl: '/assets/javascripts/rounds/views/show.html',
        controller: controllers.ShowRoundCtrl,
        resolve: {
          user: function() {
            return userResolve;
          },
          round: function($route, roundService) {
            return roundService.info($route.current.params.roundId);
          }
        }
      }).when('/rounds/amend/:roundId', {
        templateUrl: '/assets/javascripts/rounds/views/amend.html',
        controller: controllers.AmendRoundCtrl,
        resolve: userResolve
      });
  }]);

  return mod;
});
