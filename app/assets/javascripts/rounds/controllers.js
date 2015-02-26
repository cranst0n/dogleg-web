define(['lodash', 'moment', 'common'], function(_, moment) {
  'use strict';

  var RoundCatalogCtrl = function($scope, $location, $mdToast, userService, roundService) {

    $scope.rounds = [];
    $scope.num = 25;
    $scope.offset = 0;

    $scope.$watch(function() {
      var user = userService.getUser();
      return user;
    }, function(user) {
      $scope.user = user;
    }, true);

    $scope.loadRounds = function(num, offset) {
      handleRoundRequest(roundService.list(num, offset));
    };

    $scope.showRoundDetails = function(round) {
      $location.path('/rounds/show/' + round.id);
    };

    $scope.newRound = function() {
      $location.path('/rounds/new');
    };

    function handleRoundRequest(requstPromise) {

      $scope.loading = true;

      requstPromise.
        success(function(response) {
          $scope.rounds = _.map(response, function(round) {
            var withStats = roundService.calcStats(round);

            withStats.timeString = moment().format("dddd, MMMM Do YYYY");
            withStats.timeAgo = moment(withStats.time).fromNow();

            return withStats;
          });
        }).error(function(response) {
          $mdToast.showSimple(response.message);
        }).then(function() {
          $scope.loading = false;
        });
    }

    $scope.loadRounds($scope.num, $scope.offset);
  };

  RoundCatalogCtrl.$inject = ['$scope', '$location', '$mdToast', 'userService', 'roundService'];

  var EditRoundCtrl = function($scope, $location, $mdToast, $mdDialog, userService, roundService, courseService) {

    $scope.holeSets = roundService.holeSets;

    $scope.$watch('selectedCourse', function(courseSummary) {
      if(angular.isDefined(courseSummary)) {
        $scope.courseSelected(courseSummary);
      }
    }, true);

    $scope.$watch('round.course', function(course) {
      if(angular.isDefined(course)) {
        $scope.selectedCourse = course;
      }
    }, true);

    $scope.$watch('round.holeSet', function(newSet, oldSet) {
      if(angular.isDefined(newSet)) {
        $scope.round.holeSet = newSet;

        $scope.round.holeScores = initHoleScores();
      }
    }, true);

    $scope.$watch('round.holeScores', function(newScores) {
      roundService.calcStats($scope.round);
    }, true);

    $scope.$watch('round.timeDate', function(timeDate) {
      if(angular.isDefined(timeDate)) {
        $scope.round.time = timeDate.getTime();
      }
    }, true);

    $scope.$watch('round.time', function(time) {
      if(angular.isDefined(time)) {
        $scope.round.timeDate = new Date(time);
      }
    }, true);

    $scope.courseSelected = function(courseSummary) {
      courseService.info(courseSummary.id).
        success(function(response) {

          $scope.round.course = response;

          if(!angular.isDefined($scope.round.rating)) {
            $scope.round.rating = $scope.round.course.ratings[0];
          }

          courseService.calcStats($scope.round.course);

          if($scope.round.course.numHoles === 18) {
            $scope.holeSets = roundService.holeSets.slice(0);
          } else {
            $scope.holeSets = roundService.holeSets.slice(1, 2);
          }

          if(!angular.isDefined($scope.round.holeSet)) {
            $scope.round.holeSet = $scope.holeSets[0];
          }

          $scope.round.holeScores = initHoleScores();

        }).error(function(response) {
          $mdToast.showSimple(response.message);
        });
    };

    $scope.courseSearch = function(text) {
      return courseService.search(text).
        then(function(response) {
          return response.data;
        });
    };

    $scope.amendRound = function() {
      $scope.readOnly = false;
    };

    $scope.submitRound = function(round) {
      if(angular.isDefined(round.id)) {
        updateRound(round);
      } else {
        createRound(round);
      }
    };

    $scope.deleteRound = function(round, ev) {

      var confirm = $mdDialog.confirm()
        .title('Are you sure you want to delete this round?')
        .content('This can not be undone.')
        .ariaLabel('Confirm Delete')
        .ok('Yes, Delete.')
        .cancel('Cancel')
        .targetEvent(ev);

      $mdDialog.show(confirm).then(function() {
        roundService.delete(round.id).
          success(function(response) {
            $mdToast.showSimple('Round deleted.');
            $location.path('/rounds/');
          }).error(function(response) {
            $mdToast.showSimple(response.message);
          });
      });
    };

    function createRound(round) {

      // trim the fat
      var roundToSubmit = {
        courseId: round.course.id,
        ratingId: round.rating.id,
        time: round.time,
        holeScores: round.holeScores,
        official: round.official
      };

      if(!round.hasCustomHandicap) {
        delete roundToSubmit.handicapOverride;
      } else {
        roundToSubmit.handicapOverride = round.handicapOverride;
      }

      roundService.create(roundToSubmit).
        success(function(response) {
          $mdToast.showSimple('Round submitted.');
          $location.path('/rounds');
        }).error(function(response) {
          $mdToast.showSimple(response.message);
        });
    }

    function updateRound(round) {
      roundService.amend(round).
        success(function(response) {
          $mdToast.showSimple('Round amended.');
          $location.path('/rounds');
        }).error(function(response) {
          $mdToast.showSimple(response.message);
        });
    }

    function initHoleScores() {
      var holeScores = $scope.round.holeScores;

      if(angular.isDefined($scope.round.rating)) {

        for(var holeNum = $scope.round.holeSet.holeStart; holeNum <= $scope.round.holeSet.holeEnd; holeNum++) {

          var holeScore = (_.findWhere(_.pluck($scope.round.holeScores, 'hole'), { 'number': holeNum }));

          if(!angular.isDefined(holeScore)) {

            var holeRating =
              _.head(_.where($scope.round.rating.holeRatings, { 'number': holeNum }));

            holeScores.push({
              hole: _.head(_.where($scope.round.course.holes, { 'number': holeNum })),
              score: holeRating.par,
              netScore: holeRating.par,
              putts: 2,
              penaltyStrokes: 0,
              fairwayHit: holeRating.par > 3 ? true : false,
              gir: true
            });
          } else if(holeScore.par < 4) {
            holeScore.fairwayHit = false;
          }
        }
      }

      function compare(a,b) {
        if (a.hole.number < b.hole.number) {
          return -1;
        } else if (a.hole.number > b.hole.number) {
          return 1;
        } else {
          return 0;
        }
      }

      holeScores.sort(compare);

      return _.filter(holeScores, function(holeScore) {
        return holeScore.hole.number >= $scope.round.holeSet.holeStart &&
          holeScore.hole.number <= $scope.round.holeSet.holeEnd;
      });
    }
  };

  EditRoundCtrl.$inject = ['$scope', '$location','$mdToast', '$mdDialog', 'userService', 'roundService', 'courseService'];

  var NewRoundCtrl = function($scope, $location, $mdToast, roundService) {
    $scope.round = roundService.blankRound();
  };

  NewRoundCtrl.$inject = ['$scope', '$location', '$mdToast', 'roundService'];

  var ShowRoundCtrl = function($scope, $location, $mdToast, user, round, roundService) {

    $scope.round = roundService.calcStats(round.data);
    $scope.selectedCourse = $scope.round.course;

    // Angular uses referential equality so this is not ideal, but required
    for(var ix = 0; ix < $scope.round.course.ratings.length; ix++) {
      if($scope.round.rating.id === $scope.round.course.ratings[ix].id) {
        $scope.round.rating = $scope.round.course.ratings[ix];
      }
    }
  };

  ShowRoundCtrl.$inject = ['$scope', '$location', '$mdToast', 'user', 'round', 'roundService'];

  var AmendRoundCtrl = function($scope, $routeParams, $location, $mdToast, roundService) {

    $scope.round = roundService.blankRound();

    roundService.info($routeParams.roundId).
      success(function(response) {
        $scope.round = roundService.calcStats(response);

        // Angular uses referential equality so this is not ideal, but required
        for(var ix = 0; ix < $scope.round.course.ratings.length; ix++) {
          if($scope.round.rating.id === $scope.round.course.ratings[ix].id) {
            $scope.round.rating = $scope.round.course.ratings[ix];
          }
        }

      }).error(function(response) {
        $mdToast.showSimple(response.message);
      });

    $scope.amendRound = function(round) {
      roundService.amend(round).
        success(function(response) {
          $mdToast.showSimple('Round amended.');
          $location.path('/rounds');
        }).error(function(response) {
          $mdToast.showSimple(response.message);
        });
    };
  };

  AmendRoundCtrl.$inject = ['$scope', '$routeParams', '$location', '$mdToast', 'roundService'];

  return {
    RoundCatalogCtrl: RoundCatalogCtrl,
    NewRoundCtrl: NewRoundCtrl,
    EditRoundCtrl: EditRoundCtrl,
    ShowRoundCtrl: ShowRoundCtrl,
    AmendRoundCtrl: AmendRoundCtrl
  };
});
