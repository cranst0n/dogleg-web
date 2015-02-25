define(['angular', 'common'], function (angular) {
  'use strict';

  var mod = angular.module('courses.services', ['dogleg.common']);

  mod.factory('courseService', ['math', 'playRoutes', function (math, playRoutes) {

    function estimateYardage(hole) {

      var totalDistance = 0;

      for(var i = 1; i < hole.features.length; i++) {
        var lat1 = hole.features[i-1].coordinates[0].latitude;
        var lon1 = hole.features[i-1].coordinates[0].longitude;
        var lat2 = hole.features[i].coordinates[0].latitude;
        var lon2 = hole.features[i].coordinates[0].longitude;

        totalDistance += math.distanceYards(lat1, lon1, lat2, lon2);
      }

      return Math.round(totalDistance);
    }

    function estimatePar(hole) {
      var yardage = estimateYardage(hole);

      if(yardage <= 250) { return 3; }
      else if(yardage <= 470) { return 4; }
      else if(yardage <= 650) { return 5; }
      else { return 6; }
    }

    return {
      list: function(location, num, offset, approved) {
        approved = approved || true;
        var lat = angular.isDefined(location) ? location.latitude : undefined;
        var lon = angular.isDefined(location) ? location.longitude : undefined;
        return playRoutes.controllers.Courses.list(lat, lon, num, offset, approved).get();
      },
      search: function(searchText, num, offset) {
        return playRoutes.controllers.Courses.search(searchText, num, offset).get();
      },
      info: function(courseId) {
        return playRoutes.controllers.Courses.info(courseId).get();
      },
      recentForUser: function(courseId) {
        return playRoutes.controllers.Courses.recentForUser().get();
      },
      unapproved: function(num, offset) {
        return playRoutes.controllers.Courses.list(undefined, undefined, num, offset, false).get();
      },
      create: function(courseObject) {
        return playRoutes.controllers.Courses.createCourse().post(courseObject);
      },
      request: function(requestObject) {
        return playRoutes.controllers.RequestedCourses.createRequest().post(requestObject);
      },
      openRequests: function(num, offset) {
        return playRoutes.controllers.RequestedCourses.list(num, offset).get();
      },
      userRequests: function(num, offset) {
        return playRoutes.controllers.RequestedCourses.forUser(num, offset).get();
      },
      fulfillRequest: function(requestId, courseId) {
        return playRoutes.controllers.RequestedCourses.fulfill(requestId, courseId).put();
      },
      deleteRequest: function(requestId) {
        return playRoutes.controllers.RequestedCourses.deleteRequest(requestId).delete();
      },
      approve: function(courseId, course) {
        var approvalObj = {
          courseId: courseId,
          course: course
        };

        return playRoutes.controllers.Courses.approve().put(approvalObj);
      },
      parseFile: function(fileObject) {
        return playRoutes.controllers.Courses.parseFile().post(fileObject);
      },
      importFiles: function(fileObjects) {
        return playRoutes.controllers.Courses.importFiles().post(fileObjects);
      },
      lint: function(course) {
        return playRoutes.controllers.Courses.lint().post(course);
      },
      calcStats: function(course) {

        // Rating data
        for(var ix = 0; ix < course.ratings.length; ix++) {

          var rating = course.ratings[ix];
          var par = 0, yardage = 0;
          var frontPar = 0, frontYardage = 0;
          var backPar = 0, backYardage = 0;

          for(var jx = 0; jx < rating.holeRatings.length; jx++) {
            par += rating.holeRatings[jx].par;
            yardage += rating.holeRatings[jx].yardage;

            if(jx < 9) {
              frontPar += rating.holeRatings[jx].par;
              frontYardage += rating.holeRatings[jx].yardage;
            } else {
              backPar += rating.holeRatings[jx].par;
              backYardage += rating.holeRatings[jx].yardage;
            }
          }

          rating.par = par;
          rating.yardage = yardage;
          rating.frontPar = frontPar;
          rating.frontYardage = frontYardage;
          rating.backPar = backPar;
          rating.backYardage = backYardage;
        }

        return course;
      },
      ratingForHole: function(hole) {

        var holeRating =
          {
            'number': hole.number,
            'par': estimatePar(hole),
            'yardage': estimateYardage(hole),
            'handicap': 1
          };

        if(hole.number <= 9) {
          holeRating.handicap = (hole.number * 2) - 1;
        } else {
          holeRating.handicap = (hole.number - 9) * 2;
        }

        return holeRating;
      }
    };
  }]);

  return mod;
});
