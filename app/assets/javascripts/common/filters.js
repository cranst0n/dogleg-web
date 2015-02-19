/** Common filters. */
define(['angular'], function(angular) {
  'use strict';

  var mod = angular.module('common.filters', []);
  /**
   * Extracts a given property from the value it is applied to.
   * {{{
   * (user | property:'name')
   * }}}
   */
  mod.filter('property', function(value, property) {
    if (angular.isObject(value)) {
      if (value.hasOwnProperty(property)) {
        return value[property];
      }
    }
  });

  mod.filter('slice', function() {
    return function(arr, start, end) {
      if(angular.isDefined(arr)) {
        return arr.slice(start, end);
      } else {
        return [];
      }
    };
  });

  mod.filter('range', function() {
    return function(input) {

      var lowBound, highBound;

      switch (input.length) {
        case 1:
          lowBound = 0;
          highBound = parseInt(input[0]) - 1;
          break;
        case 2:
          lowBound = parseInt(input[0]);
          highBound = parseInt(input[1]);
          break;
        default:
          return input;
      }

      var result = [];

      if(lowBound <= highBound) {
        for(var i = lowBound; i <= highBound; i++) {
          result.push(i);
        }
      } else {
        for(var j = lowBound; j >= highBound; j--) {
          result.push(j);
        }
      }

      return result;
    };
  });

  return mod;
});
