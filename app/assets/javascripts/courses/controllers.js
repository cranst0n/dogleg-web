define(['common'], function() {
  'use strict';

  var CourseCatalogCtrl = function($scope, $location, $mdToast, $debounce, userService, courseService) {

    $scope.courses = [];
    $scope.num = 15;
    $scope.offset = 0;

    $scope.$watch(function() {
      var user = userService.getUser();
      return user;
    }, function(user) {
      $scope.user = user;
    }, true);

    $scope.$watch('searchText', function (newVal, oldVal) {
      if(!angular.isDefined(newVal) || (newVal.length > 1 && newVal !== oldVal)) {
        $debounce.debounce(searchRefresh, 400);
      }
    });

    function searchRefresh() {
      $scope.searchCourses($scope.searchText, $scope.num, $scope.offset);
    }

    $scope.loadCourses = function(num, offset) {
      handleCourseRequest(courseService.list(undefined, num, offset));
    };

    $scope.searchCourses = function(searchText, num, offset) {
      if(angular.isDefined(searchText) && searchText.length >= 2) {
        handleCourseRequest(courseService.search(searchText, num, offset));
      } else {
        $scope.loadCourses(num, offset);
      }
    };

    $scope.showCourseDetails = function(courseId) {
      $location.path('/courses/show/' + courseId);
    };

    $scope.previousPage = function() {
      if($scope.offset > 0) {
        $scope.offset = Math.max($scope.offset - $scope.num, 0);
        $scope.loadCourses($scope.num, $scope.offset);
      }
    };

    $scope.nextPage = function() {
      $scope.offset += $scope.num;
      $scope.loadCourses($scope.num, $scope.offset);
    };

    function handleCourseRequest(requstPromise) {
      $scope.loading = true;
      requstPromise.
        success(function(response) {
          $scope.courses = response;
        }).error(function(response) {
          $mdToast.showSimple(response.message);
        }).then(function() {
          $scope.loading = false;
        });
    }
  };

  CourseCatalogCtrl.$inject = ['$scope', '$location', '$mdToast', '$debounce', 'userService', 'courseService'];

  var NewCourseCtrl = function($scope, $location, $mdToast, userService, courseService, helper) {

    $scope.valid = false;
    $scope.inputFile = undefined;

    $scope.$watch(function() {
      var user = userService.getUser();
      return user;
    }, function(user) {
      $scope.user = user;
    }, true);

    $scope.$watch('inputFile', function(newFile) {
      $scope.course = undefined;

      if(angular.isDefined(newFile) && newFile[0]) {
        helper.createStringFile(newFile[0]).
          then(function(fileObject) {
            courseService.parseFile(fileObject).
              success(function(response) {
                $scope.course = response;
              }).error(function(response) {
                $mdToast.showSimple(response.message);
              });
          });
      }
    });

    $scope.$watch('course', function(newCourse, oldCourse) {
      if(angular.isDefined(newCourse)) {
        courseService.calcStats(newCourse);

        $scope.valid =
          angular.isDefined(newCourse.name) &&
          angular.isDefined(newCourse.city) &&
          angular.isDefined(newCourse.state) &&
          angular.isDefined(newCourse.country) &&
          newCourse.numHoles === 9 || newCourse.numHoles === 18 &&
          angular.isDefined(newCourse.ratings) && newCourse.ratings.length > 0;

      } else {
        $scope.valid = false;
      }

    }, true);

    $scope.submitCourse = function(courseObject) {
      courseService.create(courseObject).
        success(function(response) {
          $mdToast.showSimple('Course submitted. Administrator must approve it.');
          $location.path('/courses');
        }).error(function(response) {
          $mdToast.showSimple(response.message);
        });
    };
  };

  NewCourseCtrl.$inject = ['$scope', '$location', '$mdToast', 'userService', 'courseService', 'helper'];

  var CourseCardCtrl = function($scope, $mdToast, $mdDialog, courseService) {

    $scope.promptRating = function(ev) {
      promptRatingName(ev).then(function(answer) {
        addRating(answer);
      });
    };

    $scope.copyRating = function(rating, ev) {
      promptRatingName(ev).then(function(answer) {
        addRating(answer, rating);
      });
    };

    $scope.removeRating = function(idx) {
      if ($scope.course.ratings.length > idx) {
        $scope.course.ratings.splice(idx, 1);
      }
    };

    $scope.lintCourse = function(course) {

      $scope.courseLint = [];

      courseService.lint(course).
        success(function(response) {
          $scope.courseLint = response;
        }).error(function(response) {
          $mdToast.showSimple(response.message);
        });
    };

    function promptRatingName(ev) {
      return $mdDialog.show({
        controller: function DialogController($scope, $mdDialog) {
          $scope.cancel = function() {
            $mdDialog.cancel();
          };
          $scope.answer = function(answer) {
            $mdDialog.hide(answer);
          };
        },
        templateUrl: '/assets/javascripts/courses/views/ratingNameDialog.html',
        onComplete: function() {
          angular.element(document.querySelector('[ng-model="ratingName"]')).focus();
        },
        targetEvent: ev
      });
    }

    function addRating(ratingName, ratingPrototype) {

      var newRating = angular.copy(ratingPrototype) || {
        rating: 72,
        slope: 113,
        frontRating: 36,
        frontSlope: 113,
        backRating: 36,
        backSlope: 113,
        bogeyRating: 90,
        gender: "Male"
      };

      newRating.teeName = ratingName;

      if(!angular.isDefined(ratingPrototype)) {
        newRating.holeRatings = [];

        for(var holeNum = 0; holeNum < $scope.course.numHoles; holeNum++) {
          var hole = $scope.course.holes[holeNum];
          newRating.holeRatings.push(courseService.ratingForHole(hole));
        }
      }

      $scope.course.ratings.push(newRating);
    }
  };

  CourseCardCtrl.$inject = ['$scope', '$mdToast', '$mdDialog', 'courseService'];

  var RequestCourseCtrl = function($scope, $location, $mdToast, courseService) {

    $scope.submitRequest = function(request) {
      courseService.request(request).
        success(function(response) {
          $mdToast.showSimple('Request submitted!');
          $location.path('/courses');
        }).error(function(response) {
          $mdToast.showSimple(response.message);
          $scope.courseRequestError = response;
        });
    };
  };

  RequestCourseCtrl.$inject = ['$scope', '$location', '$mdToast', 'courseService'];

  var UserRecentCoursesListCtrl = function($scope, courseService) {

    function refresh() {
      $scope.loading = true;
      $scope.recentCourses = [];
      courseService.recentForUser().success(
        function(response) {
          $scope.recentCourses = response;
        }
      ).then(function() {
        $scope.loading = false;
      });
    }

    refresh();
  };

  UserRecentCoursesListCtrl.$inject = ['$scope', 'courseService'];

  var UserRequestedCoursesListCtrl = function($scope, $location, $mdToast, courseService) {

    $scope.requestNew = function() {
      $location.path('/courses/request');
    };

    courseService.userRequests().
      success(function(response) {
        $scope.requestedCourses = response;
      }).error(function(response) {
        $mdToast.showSimple(response.message);
      });
  };

  UserRequestedCoursesListCtrl.$inject = ['$scope', '$location', '$mdToast', 'courseService'];

  var ShowCourseCtrl = function($scope, $routeParams, $mdToast, courseService) {

    courseService.info($routeParams.courseId).
      success(function(response) {
        $scope.course = response;
        courseService.calcStats($scope.course);
      }).error(function(response) {
        $mdToast.showSimple(response.message);
      });
  };

  ShowCourseCtrl.$inject = ['$scope', '$routeParams', '$mdToast', 'courseService'];

  return {
    CourseCatalogCtrl: CourseCatalogCtrl,
    NewCourseCtrl: NewCourseCtrl,
    CourseCardCtrl: CourseCardCtrl,
    RequestCourseCtrl: RequestCourseCtrl,
    UserRecentCoursesListCtrl: UserRecentCoursesListCtrl,
    UserRequestedCoursesListCtrl: UserRequestedCoursesListCtrl,
    ShowCourseCtrl: ShowCourseCtrl
  };

});
