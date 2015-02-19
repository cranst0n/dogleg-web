define(['angular', './controllers'], function(angular, controllers) {
  'use strict';

  var mod = angular.module('rounds.directives', []);

  mod.directive('scoreCard', function() {
    return {
      restrict: 'E',
      templateUrl: '/assets/javascripts/rounds/views/scoreCard.html',
      scope: {
        'round': '='
      }
    };
  });

  mod.directive('editRound', function() {
    return {
      restrict: 'E',
      templateUrl: '/assets/javascripts/rounds/views/edit.html',
      controller: controllers.EditRoundCtrl,
      scope: {
        'round': '=',
        'readOnly': '='
      }
    };
  });

  return mod;
});
