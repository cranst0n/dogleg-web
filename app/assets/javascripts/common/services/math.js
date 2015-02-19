/** Common maths */
define(['angular'], function(angular) {
  'use strict';

  var mod = angular.module('common.math', []);

  mod.service('math', function() {

    function distanceYards(lat1, lon1, lat2, lon2) {
      return distance(lat1, lon1, lat2, lon2) * 1093.61;
    }

    function distance(lat1, lon1, lat2, lon2) {
      var R = 6372.8; // km (change this constant to get miles)
      var dLat = (lat2 - lat1) * Math.PI / 180;
      var dLon = (lon2 - lon1) * Math.PI / 180;

      var a = Math.sin(dLat/2) * Math.sin(dLat/2) +
        Math.cos(lat1 * Math.PI / 180 ) * Math.cos(lat2 * Math.PI / 180 ) *
        Math.sin(dLon/2) * Math.sin(dLon/2);

      var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
      var d = R * c;

      return d;
    }

    return {
      distanceYards: distanceYards,
      distance: distance
    };
  });

  return mod;
});
