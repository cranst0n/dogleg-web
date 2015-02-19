var DoglegHomepage = function() {

  this.showLoginButton = element(by.css('[data-ng-click="showLogin()"]'));
  this.loginUsernameInput = element(by.model('credentials.username'));
  this.loginPasswordInput = element(by.model('credentials.password'));
  this.loginErrorDisplay = element(by.css('[ng-if="loginError"]'));

  this.userMenuButton = element(by.css('.user-dropdown'));

  this.tryLoginButton = element(by.css('[data-ng-click="login(credentials)"]'));
  this.tryLogoutButton = element(by.css('[data-ng-click="logout()"]'));

  this.goto = function() {
    browser.get(browser.params.site.baseUrl);
  };

  this.promptLogin = function() {
    this.showLoginButton.click();
  };

  this.setLoginUsername = function(name) {
    this.loginUsernameInput.sendKeys(name);
  };

  this.setLoginPassword = function(password) {
    this.loginPasswordInput.sendKeys(password);
  };

  this.tryLogin = function(name, password) {
    this.goto();
    this.promptLogin();
    this.setLoginUsername(name);
    this.setLoginPassword(password);
    this.doLogin();
  }

  this.doLogin = function() {
    this.tryLoginButton.click();
  }

  this.doLogout = function() {
    this.userMenuButton.click();
    this.tryLogoutButton.click();
  }
};

module.exports = DoglegHomepage;
