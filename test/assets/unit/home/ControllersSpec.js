define(['angular','angular-mocks','app'], function(angular,ngMocks,app) {

  'use strict';

  describe('HeaderCtrl', function() {

    var scope, location, controller, mockUserService, mockLXDialogService;

    beforeEach(module('dogleg.home'));

    beforeEach(function () {
      mockUserService = jasmine.createSpyObj('userService', ['getUser','logout']);
      mockUserService.getUser.and.returnValue({
        id: 1, name: "dogleg", password: "", email: "dogleg@dogleg",
        admin: true, active: true, created: 1234
      });

      mockLXDialogService = jasmine.createSpyObj('LxDialogService',['open']);
    });

    beforeEach(inject(function ($rootScope, $location, $controller) {

      scope = $rootScope.$new();
      location = $location;

      controller = $controller('HeaderCtrl', {
        $rootScope: $rootScope,
        $scope: scope,
        userService: mockUserService,
        $location: $location,
        LxDialogService: mockLXDialogService
      });

      scope.$digest(); // trigger the watch
    }));

    it('watches the current user.', function() {
      expect(scope.user.id).toBe(1);
      expect(mockUserService.getUser).toHaveBeenCalled();
    });

    it('logs the current user out.', function() {
      scope.logout();

      expect(scope.user).toBe(undefined);
      expect(mockUserService.logout).toHaveBeenCalled();
    });

    it('shows the login dialog.', function() {
      scope.showLogin();

      expect(mockLXDialogService.open).toHaveBeenCalledWith('loginDialog');
    });

  });
});