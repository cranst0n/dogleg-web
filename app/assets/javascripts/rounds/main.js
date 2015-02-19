define(['angular', './routes', './controllers', './services', './directives'], function(angular, routes, controllers, services, directives) {
  'use strict';

  var mod = angular.module('dogleg.rounds', ['ngRoute', 'common.debounce', 'rounds.routes', 'rounds.services', 'rounds.directives']);

  mod.controller('RoundCatalogCtrl', controllers.RoundCatalogCtrl);
  mod.controller('NewRoundCtrl', controllers.NewRoundCtrl);
  mod.controller('ShowRoundCtrl', controllers.ShowRoundCtrl);
  mod.controller('AmendRoundCtrl', controllers.AmendRoundCtrl);

  return mod;
});
