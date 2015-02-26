define(['angular', 'lodash', 'common'], function (angular, _) {
  'use strict';

  var mod = angular.module('rounds.services', ['dogleg.common']);

  var holeSets = [
    { title: '18 Holes', holeStart: 1, holeEnd: 18, numHoles: 18 },
    { title: 'Front 9', holeStart: 1, holeEnd: 9, numHoles: 9 },
    { title: 'Back 9', holeStart: 10, holeEnd: 18, numHoles: 9 }
  ];

  mod.factory('roundService', ['playRoutes', function (playRoutes) {

    function calcStats(round) {

      round.frontPar = 0;
      round.backPar = 0;

      round.score = 0;
      round.scoreToPar = 0;
      round.putts = 0;
      round.penaltyStrokes = 0;
      round.fairwaysHit = 0;
      round.fairwaysHitPossible = 0;
      round.girs = 0;

      round.frontScore = 0;
      round.frontPutts = 0;
      round.frontPenaltyStrokes = 0;
      round.frontFairwaysHit = 0;
      round.frontFairwaysHitPossible = 0;
      round.frontGIRs = 0;

      round.backScore = 0;
      round.backPutts = 0;
      round.backPenaltyStrokes = 0;
      round.backFairwaysHit = 0;
      round.backFairwaysHitPossible = 0;
      round.backGIRs = 0;

      if(angular.isDefined(round.holeScores) && round.holeScores.length > 0) {

        var holeStart = _.min(round.holeScores, function(score) { return score.hole.number; }).hole.number;
        var holeEnd = _.max(round.holeScores, function(score) { return score.hole.number; }).hole.number;

        if(holeStart === 10) {
          round.holeSet = holeSets[2];
        } else if(holeEnd === 18) {
          round.holeSet = holeSets[0];
        } else {
          round.holeSet = holeSets[1];
        }

        if(angular.isDefined(round.course)) {
          for(var holeNum = round.holeSet.holeStart; holeNum <= round.holeSet.holeEnd; holeNum++) {

            var holeIndex = holeNum - round.holeSet.holeStart;
            var holeRating = round.rating.holeRatings[holeIndex];

            round.scoreToPar += (round.holeScores[holeIndex].score - holeRating.par);
            round.score += round.holeScores[holeIndex].score;
            round.putts += round.holeScores[holeIndex].putts;
            round.penaltyStrokes += round.holeScores[holeIndex].penaltyStrokes;

            if(holeRating.par >= 4) {
              round.fairwaysHitPossible++;
            }

            if(round.holeScores[holeIndex].fairwayHit) {
              round.fairwaysHit++;
            }

            if(round.holeScores[holeIndex].gir) {
              round.girs++;
            }

            if(holeNum <= 9) {
              round.frontPar += holeRating.par;
              round.frontScore += round.holeScores[holeIndex].score;
              round.frontPutts += round.holeScores[holeIndex].putts;
              round.frontPenaltyStrokes += round.holeScores[holeIndex].penaltyStrokes;

              if(holeRating.par >= 4) {
                round.frontFairwaysHitPossible++;
              }

              if(round.holeScores[holeIndex].fairwayHit) {
                round.frontFairwaysHit++;
              }

              if(round.holeScores[holeIndex].gir) {
                round.frontGIRs++;
              }
            } else {
              round.backPar += holeRating.par;
              round.backScore += round.holeScores[holeIndex].score;
              round.backPutts += round.holeScores[holeIndex].putts;
              round.backPenaltyStrokes += round.holeScores[holeIndex].penaltyStrokes;

              if(holeRating.par >= 4) {
                round.backFairwaysHitPossible++;
              }

              if(round.holeScores[holeIndex].fairwayHit) {
                round.backFairwaysHit++;
              }

              if(round.holeScores[holeIndex].gir) {
                round.backGIRs++;
              }
            }
          }
        }

        round.fairwaysHitPercent = ((round.fairwaysHit / round.fairwaysHitPossible) * 100).toFixed(0);
        round.girPercent = ((round.girs / round.numHoles) * 100).toFixed(0);

        round.frontFairwaysHitPercent = ((round.frontFairwaysHit / round.frontFairwaysHitPossible) * 100).toFixed(0);
        round.backFairwaysHitPercent = ((round.backFairwaysHit / round.backFairwaysHitPossible) * 100).toFixed(0);

        round.frontGIRPercent = ((round.frontGIRs / 9.0) * 100).toFixed(0);
        round.backGIRPercent = ((round.backGIRs / 9.0) * 100).toFixed(0);

        if (round.scoreToPar > 0) {
          round.scoreToPar = '+' + round.scoreToPar;
        } else if(round.scoreToPar === 0) {
          round.scoreToPar = 'E';
        }
      }

      return round;
    }

    return {
      info: function(id) {
        return playRoutes.controllers.Rounds.info(id).get();
      },
      list: function(num, offset) {
        return playRoutes.controllers.Rounds.list(num, offset, true).get();
      },
      create: function(roundObject) {
        return playRoutes.controllers.Rounds.createRound().post(roundObject);
      },
      amend: function(roundObject) {
        return playRoutes.controllers.Rounds.updateRound().put(roundObject);
      },
      delete: function(roundId) {
        return playRoutes.controllers.Rounds.deleteRound(roundId).delete();
      },
      calcStats: calcStats,
      holeSets: holeSets,
      blankRound: function() {
        return {
          holeSet: holeSets[0],
          holeScores: [],
          official: true
        };
      }
    };
  }]);

  return mod;
});
