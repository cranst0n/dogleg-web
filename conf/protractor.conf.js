exports.config = {
  seleniumAddress: 'http://localhost:4444/wd/hub',

  multiCapabilities: [
    { 'browserName' : 'chrome' },
    // { 'browserName' : 'firefox' }
  ],

  params: {
    site: {
      baseUrl: 'http://localhost:9000'
    },
    login: {
      user: 'dogleg',
      password: 'dogleg'
    }
  },

  suites: {
    homepage: '../test/assets/e2e/homepage/**/*.js'
  },

  jasmineNodeOpts: {
    showColors: true
  }
};
