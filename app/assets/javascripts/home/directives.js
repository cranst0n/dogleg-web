define(['angular', './controllers'], function(angular, controllers) {
  'use strict';

  var mod = angular.module('home.directives', []);

  mod.directive('navbar', function() {
    return {
      restrict: 'E',
      templateUrl: '/assets/javascripts/home/views/navbar.html'
    };
  });

  return mod;
});
