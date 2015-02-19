/**
 * Common functionality.
 */
define(['angular', './services/math', './services/helper',
  './services/playRoutes', './services/debounce', './filters',
  './directives/general'], function(angular) {
  'use strict';

  return angular.module('dogleg.common', ['common.math', 'common.helper',
    'common.debounce', 'common.playRoutes', 'common.filters',
    'common.directives.general']);
});
