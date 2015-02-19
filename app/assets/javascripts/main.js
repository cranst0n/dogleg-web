// `main.js` is the file that sbt-web will use as an entry point
(function (requirejs) {
  'use strict';

  // -- RequireJS config --
  requirejs.config({
    // Packages = top-level folders; loads a contained file named 'main.js"
    packages: ['common', 'home', 'user', 'courses', 'rounds', 'admin'],
    shim: {
      'jsRoutes': {
        deps: [],
        // it's not a RequireJS module, so we have to tell it what var is returned
        exports: 'jsRoutes'
      },
      // Hopefully this all will not be necessary but can be fetched from WebJars in the future
      'angular': {
        exports: 'angular'
      },
      'angular-route':            ['angular'],
      'angular-animate':          ['angular'],
      'angular-cookies':          ['angular'],
      'angular-aria':             ['angular'],
      'angular-material':         ['angular'],
      'md-date-time':             ['angular-material']
    },
    paths: {
      'requirejs':                '../lib/requirejs/require',
      'jquery':                   '../lib/jquery/jquery',
      'angular':                  '../lib/angularjs/angular',
      'angular-route':            '../lib/angularjs/angular-route',
      'angular-animate':          '../lib/angularjs/angular-animate',
      'angular-cookies':          '../lib/angularjs/angular-cookies',
      'angular-aria':             '../lib/angularjs/angular-aria',
      'angular-material':         '//rawgit.com/angular/bower-material/master/angular-material',
      'md-date-time':             '//rawgit.com/SimeonC/md-date-time/master/dist/md-date-time',
      'lodash' :                  '../lib/lodash/lodash',
      'moment':                   '../lib/momentjs/min/moment-with-locales',
      'CryptoJS':                 '../lib/cryptojs/rollups/sha3',
      'jsRoutes':                 '/jsroutes'
    }
  });

  requirejs.onError = function (err) {
    console.log(err);
  };

  // Load the app. This is kept minimal so it doesn't need much updating.
  require(['angular', 'angular-route', 'angular-animate', 'angular-cookies', 'angular-aria', 'angular-material', 'md-date-time', './app'],
    function (angular) {
      angular.bootstrap(document, ['app', 'ngMaterial', 'mdDateTime']);
    }
  );
})(requirejs);
