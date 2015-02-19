define(['angular', 'lodash', 'moment'], function(angular, _, moment) {
  'use strict';

  var mod = angular.module('common.directives.general', []);

  mod.directive('ngEnter', function() {
    return function (scope, element, attrs) {
      element.bind("keydown keypress", function (event) {
        if(event.which === 13) {
          scope.$apply(function (){
            scope.$eval(attrs.ngEnter);
          });

          event.preventDefault();
        }
      });
    };
  });

  mod.directive('fileread', [function () {
    return {
      scope: {
        fileread: "="
      },
      link: function (scope, element, attributes) {
        element.bind("change", function (changeEvent) {
          scope.$apply(function () {
            scope.fileread = changeEvent.target.files;
          });
        });
      }
    };
  }]);

  mod.directive('dateTimePicker', ['$mdDialog', function($mdDialog) {
    return {
      restrict: 'E',
      scope: {
        value: '=',
        label: '@',
        format: '@'
      },
      templateUrl: '/assets/javascripts/common/views/dateTimePicker.html',
      controller: function($scope) {

        if(!angular.isDefined($scope.value)) {
          $scope.value = new Date();
        }

        $scope.$watch('value', function(newValue) {
          if(angular.isDefined(newValue)) {
            $scope.valueString = moment(newValue).format($scope.format);
          }
        });

        $scope.showPickerDialog = function(ev) {
          $mdDialog.show({
            template:
              '<md-dialog aria-label="Date/Time Picker" flex>' +
                '<time-date-picker ng-model="value" on-save="saved($value)" on-cancel="cancelled()"></time-date-picker>' +
              '</md-content>',
            controller: function($scope, $mdDialog) {

              $scope.saved = function($value) {
                $mdDialog.hide($value);
              };

              $scope.cancelled = function() {
                console.log('cancelled');
                $mdDialog.hide();
              };
            },
            targetEvent: ev
          }).then(function(pickedDate) {
            if(angular.isDefined(pickedDate)) {
              $scope.value = pickedDate;
            }
          });
        };
      }
    };
  }]);

  return mod;
});
