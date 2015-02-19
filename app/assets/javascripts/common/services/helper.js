/** Common helpers */
define(['angular'], function(angular) {
  'use strict';

  var mod = angular.module('common.helper', []);

  mod.service('helper', ['$window', '$q', function($window, $q) {

    // http://stackoverflow.com/questions/9267899/arraybuffer-to-base64-encoded-string
    function readBase64(buffer) {
      var binary = '';
      var bytes = new Uint8Array(buffer);
      var len = bytes.byteLength;
      for (var i = 0; i < len; i++) {
        binary += String.fromCharCode(bytes[i]);
      }
      return $window.btoa(binary);
    }

    return {
      createBase64File: function(file) {

        var deferred = $q.defer();
        var reader = new FileReader();

        reader.onload = function(e) {
          deferred.resolve({
            'filetype' : file.type,
            'filename' : file.name,
            'filesize' : file.size,
            'content'  : readBase64(e.target.result)
          });
        };

        reader.readAsArrayBuffer(file);

        return deferred.promise;
      },
      createStringFile: function(file) {

        var deferred = $q.defer();
        var reader = new FileReader();

        reader.onload = function(e) {
          deferred.resolve({
            'filetype' : file.type,
            'filename' : file.name,
            'filesize' : file.size,
            'content'  : e.target.result
          });
        };

        reader.readAsText(file);

        return deferred.promise;
      },
      fileExtension: function(filename) {

        var a = filename.split(".");

        if( a.length === 1 || ( a[0] === "" && a.length === 2 ) ) {
            return "";
        }

        return a.pop().toLowerCase();
      }
    };
  }]);

  return mod;
});
