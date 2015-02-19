var allTestFiles = [];
var TEST_REGEXP = /(spec|test)\.js$/i;

var pathToModule = function(path) {
  return path.replace(/^\/base\//, '').replace(/\.js$/, '');
};

Object.keys(window.__karma__.files).forEach(function(file) {
  if (TEST_REGEXP.test(file)) {
    // Normalize paths to RequireJS module names.
    allTestFiles.push('../../../' + pathToModule(file));
  }
});

var appDir = '/base/app/assets/javascripts';

var baseProjectDir = '../../../';
var webJarDir = baseProjectDir + 'target/web/web-modules/main/webjars/lib/';

require.config({
  // Karma serves files under /base, which is the basePath from your config file
  baseUrl: appDir,

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
    'angular-mocks':            ['angular'],
    'velocity':                 ['jquery'],
    'lumx':                     ['jquery','velocity','moment','angular']
  },
  paths: {
    'jquery':              webJarDir + 'jquery/jquery',
    'angular':             webJarDir + 'angularjs/angular',
    'angular-route':       webJarDir + 'angularjs/angular-route',
    'angular-animate':     webJarDir + 'angularjs/angular-animate',
    'angular-cookies':     webJarDir + 'angularjs/angular-cookies',
    'angular-aria':        webJarDir + 'angularjs/angular-aria',
    'angular-mocks':       webJarDir + 'angularjs/angular-mocks',
    'lodash':              webJarDir + 'lodash/lodash',
    'velocity':            webJarDir + 'velocity/velocity',
    'moment':              webJarDir + 'momentjs/min/moment-with-locales',
    'CryptoJS':            webJarDir + 'cryptojs/rollups/sha3',
    'lumx':                '//cdn.rawgit.com/lumapps/lumX/v0.2.47/dist/js/lumx',
    'jsRoutes':            baseProjectDir + 'test/assets/unit/jsRoutes'
  },

  priority: [
    "angular"
  ],

  // dynamically load all test files
  deps: allTestFiles,

  // we have to kickoff jasmine, as it is asynchronous
  callback: window.__karma__.start
});
