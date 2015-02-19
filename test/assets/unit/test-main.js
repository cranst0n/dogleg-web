var tests = [];
for (var file in window.__karma__.files) {
  if (window.__karma__.files.hasOwnProperty(file)) {
    if (/Spec\.js$/.test(file)) {
      tests.push(file);
    }
  }
}

var libPrefix = '../target/web/web-modules/main/webjars/lib/';

requirejs.config({

  // Karma serves files from '/base'
  baseUrl: '/base',

  packages: ['common', 'home', 'user', 'courses'],
  shim: {
    'jsRoutes': {
      deps: [],
      // it's not a RequireJS module, so we have to tell it what var is returned
      exports: 'jsRoutes'
    },
    // Hopefully this all will not be necessary but can be fetched from WebJars in the future
    'angular': {
      deps: ['jquery'],
      exports: 'angular'
    },
    'angular-route':            ['angular'],
    'angular-animate':          ['angular'],
    'angular-cookies':          ['angular'],
    'angular-aria':             ['angular'],
    'velocity':                 ['jquery'],
    'lumx':                     ['jquery','velocity','moment','angular']
  },
  paths: {
    'jquery':              libPrefix + 'jquery/jquery',
    'angular':             libPrefix + 'angularjs/angular',
    'angular-route':       libPrefix + 'angularjs/angular-route',
    'angular-animate':     libPrefix + 'angularjs/angular-animate',
    'angular-cookies':     libPrefix + 'angularjs/angular-cookies',
    'angular-aria':        libPrefix + 'angularjs/angular-aria',
    // 'angular-mocks':       '../../../target/web/web-modules/main/webjars/lib/' + 'angularjs/angular-mocks',
    'lodash':              libPrefix + 'lodash/lodash',
    'velocity':            libPrefix + 'velocity/velocity',
    'moment':              libPrefix + 'momentjs/min/moment-with-locales',
    'CryptoJS':            libPrefix + 'cryptojs/rollups/sha3',
    'lumx':                '//cdn.rawgit.com/lumapps/lumX/v0.2.47/dist/js/lumx',

    'app': '../../../app/assets/javascripts/app'
  },

  priority: [
    "angular"
  ],

  // ask Require.js to load these files (all our tests)
  deps: tests,

  // start test run, once Require.js is done
  callback: window.__karma__.start
});