
var DoglegHomepage = require('./homepage.po.js');

describe('login page', function() {

  var params = browser.params;
  var homepage = new DoglegHomepage();

  homepage.goto();

  it('should login successfully', function() {

    homepage.tryLogin(params.login.user,params.login.password);

    expect(browser.getCurrentUrl()).toEqual(params.site.baseUrl + '/#/');

    homepage.doLogout();
  });

  it('should notify failed login', function() {

    homepage.tryLogin(params.login.user + '2', params.login.password);

    expect(homepage.loginErrorDisplay.isDisplayed()).toBeTruthy();
  });

});