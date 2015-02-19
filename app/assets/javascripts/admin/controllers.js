/**
 * Home controllers.
 */
define(['lodash'], function(_) {
  'use strict';

  var AdminCtrl = function($scope) {

  };

  AdminCtrl.$inject = ['$scope'];

  var UnapprovedListCtrl = function($scope, $mdToast, $mdDialog, courseService) {

    $scope.unapprovedCourses = [];

    function refresh() {

      $scope.unapprovedCourses = [];
      $scope.loading = true;

      courseService.unapproved(20,0).
        success(function(response) {
          $scope.unapprovedCourses = response;
        }).error(function(response) {
          $mdToast.showSimple(response.message);
        }).then(function() {
          $scope.loading = false;
        });
    }

    $scope.showCourseDetails = function(courseId, ev) {
      $scope.loading = true;
      courseService.info(courseId).
        success(function(response) {

          $mdDialog.show({
            template:
              '<md-dialog aria-label="Course Information">' +
                '<course-card course="courseToReview" read-only="false"></course-card>' +
                '<div class="md-actions" layout="row">' +
                  '<span flex></span>' +
                  '<md-button ng-click="cancel()">Cancel</md-button>' +
                  '<md-button ng-click="rejectCourse(courseToReview)" class="md-warn md-raised">Reject</md-button>' +
                  '<md-button ng-click="approveCourse(courseToReview)" class="md-primary md-raised">Approve</md-button>' +
                '</div>' +
              '</md-dialog>',
            controller: function($scope, course) {

              $scope.courseToReview = course;

              $scope.approveCourse = function(course) {
                courseService.approve(course.id, course).
                  success(function(response) {
                    $mdDialog.hide();
                    $mdToast.showSimple('Course Approved');
                    refresh();
                  }).error(function(response) {
                    $mdToast.showSimple(response.message);
                  });
              };

              $scope.rejectCourse = function(course) {
                $mdDialog.hide();
                $mdToast.showSimple('Not Implemented.');
              };

              $scope.cancel = function() {
                $mdDialog.hide();
              };
            },
            locals: {
              course: courseService.calcStats(response)
            },
            targetEvent: ev
          });
        }).error(function(response) {
          $mdToast.showSimple(response.message);
        }).then(function() {
          $scope.loading = false;
        });
    };

    refresh();
  };

  UnapprovedListCtrl.$inject = ['$scope', '$mdToast', '$mdDialog', 'courseService'];

  var RequestedCourseListCtrl = function($scope, $mdToast, $mdDialog, courseService) {

    $scope.requestedCourses = [];

    function refresh() {

      $scope.requestedCourses = [];
      $scope.loading = true;

      courseService.openRequests(20,0).
        success(function(response) {
          $scope.requestedCourses = response;
        }).error(function(response) {
          $mdToast.showSimple(response.message);
        }).then(function() {
          $scope.loading = false;
        });
    }

    $scope.showFulfillmentDialog = function(requestToFulfill, ev) {

      $mdDialog.show({
        templateUrl: '/assets/javascripts/admin/views/fulfillRequestDialog.html',
        controller: function DialogController($scope, $mdDialog, request) {

          $scope.request = request;

          $scope.fulfillRequest = function(requestId, courseId) {

            courseService.fulfillRequest(requestId, courseId).
              success(function(response) {
                refresh();
              }).error(function(response) {
                $mdToast.showSimple(response.message);
              }).then(function() {
                $mdDialog.hide();
              });
          };

          $scope.close = function() {
            $mdDialog.hide();
          };

          $scope.courseSearch = function(text) {
            return courseService.search(text).
              then(function(response) {
                return response.data;
              });
          };
        },
        locals: {
          request: requestToFulfill
        },
        targetEvent: ev
      });
    };

    $scope.rejectRequest = function(requestId) {

      $scope.loading = true;

      courseService.deleteRequest(requestId).
        success(function(response) {
          refresh();
        }).error(function(response) {
          $mdToast.showSimple(response.message);
        }).then(function() {
          $scope.loading = false;
        });
    };

    refresh();
  };

  RequestedCourseListCtrl.$inject = ['$scope', '$mdToast', '$mdDialog', 'courseService'];

  var QuickCourseImportCtrl = function($scope, $mdToast, courseService, helper) {

    $scope.inputFiles = undefined;
    $scope.filesToImport = [];

    $scope.importing = false;

    $scope.$watch('inputFiles', function(newFiles) {
      if(angular.isDefined(newFiles)) {

        $scope.filesToImport = [];

        for(var ix = 0; ix < newFiles.length; ix++) {
          var fileObj = newFiles[ix];
          if(helper.fileExtension(fileObj.name) === "zip") {
            helper.createBase64File(fileObj).then(addImportFile);
          } else {
            helper.createStringFile(fileObj).then(addImportFile);
          }
        }
      }
    });

    $scope.importCourses = function(courses) {

      $scope.importing = true;

      courseService.importFiles(courses).
        success(function(response) {
          $scope.filesToImport = [];
          $mdToast.showSimple(response.message);
        }).error(function(response) {
          $mdToast.showSimple(response.message);
        }).then(function() {
          $scope.importing = false;
        });
    };

    function addImportFile(file) {
      $scope.filesToImport.push(file);
    }
  };

  QuickCourseImportCtrl.$inject = ['$scope', '$mdToast', 'courseService', 'helper'];

  return {
    AdminCtrl: AdminCtrl,
    UnapprovedListCtrl: UnapprovedListCtrl,
    RequestedCourseListCtrl: RequestedCourseListCtrl,
    QuickCourseImportCtrl: QuickCourseImportCtrl
  };

});
