var jsRoutes = {}; (function(_root){
var _nS = function(c,f,b){var e=c.split(f||"."),g=b||_root,d,a;for(d=0,a=e.length;d<a;d++){g=g[e[d]]=g[e[d]]||{}}return g}
var _qS = function(items){var qs = ''; for(var i=0;i<items.length;i++) {if(items[i]) qs += (qs ? '&' : '') + items[i]}; return qs ? ('?' + qs) : ''}
var _s = function(p,s){return p+((s===true||(s&&s.secure))?'s':'')+'://'}
var _wA = function(r){return {ajax:function(c){c=c||{};c.url=r.url;c.type=r.method;return jQuery.ajax(c)}, method:r.method,type:r.method,url:r.url,absoluteURL: function(s){return _s('http',s)+'localhost:9000'+r.url},webSocketURL: function(s){return _s('ws',s)+'localhost:9000'+r.url}}}
_nS('controllers.Assets'); _root.controllers.Assets.versioned =
      function(file) {
      return _wA({method:"GET", url:"/" + "assets/" + (function(k,v) {return v})("file", file)})
      }

_nS('controllers.Authentication'); _root.controllers.Authentication.login =
      function() {
      return _wA({method:"POST", url:"/" + "login"})
      }

_nS('controllers.Authentication'); _root.controllers.Authentication.logout =
      function() {
      return _wA({method:"POST", url:"/" + "logout"})
      }

_nS('controllers.Authentication'); _root.controllers.Authentication.authUser =
      function() {
      return _wA({method:"GET", url:"/" + "authuser"})
      }

_nS('controllers.Authentication'); _root.controllers.Authentication.authAdmin =
      function() {
      return _wA({method:"GET", url:"/" + "authadmin"})
      }

_nS('controllers.Users'); _root.controllers.Users.createUser =
      function() {
      return _wA({method:"POST", url:"/" + "users"})
      }

_nS('controllers.Users'); _root.controllers.Users.updateUser =
      function(id) {
      return _wA({method:"PUT", url:"/" + "users/" + (function(k,v) {return v})("id", id)})
      }

_nS('controllers.Users'); _root.controllers.Users.changePassword =
      function(id) {
      return _wA({method:"PUT", url:"/" + "users/" + (function(k,v) {return v})("id", id) + "/password"})
      }

_nS('controllers.Users'); _root.controllers.Users.changeAvatar =
      function(id) {
      return _wA({method:"PUT", url:"/" + "users/" + (function(k,v) {return v})("id", id) + "/avatar"})
      }

_nS('controllers.Users'); _root.controllers.Users.deleteUser =
      function(id) {
      return _wA({method:"DELETE", url:"/" + "users/" + (function(k,v) {return v})("id", id)})
      }

_nS('controllers.Users'); _root.controllers.Users.user =
      function(id) {
      return _wA({method:"GET", url:"/" + "users/" + (function(k,v) {return v})("id", id)})
      }

_nS('controllers.Rounds'); _root.controllers.Rounds.createRound =
      function() {
      return _wA({method:"POST", url:"/" + "rounds"})
      }

_nS('controllers.Rounds'); _root.controllers.Rounds.updateRound =
      function() {
      return _wA({method:"PUT", url:"/" + "rounds"})
      }

_nS('controllers.Rounds'); _root.controllers.Rounds.deleteRound =
      function(id) {
      return _wA({method:"DELETE", url:"/" + "rounds/" + (function(k,v) {return v})("id", id)})
      }

_nS('controllers.Rounds'); _root.controllers.Rounds.info =
      function(id) {
      return _wA({method:"GET", url:"/" + "rounds/" + (function(k,v) {return v})("id", id)})
      }

_nS('controllers.Rounds'); _root.controllers.Rounds.list =
      function(num,offset) {
      return _wA({method:"GET", url:"/" + "rounds" + _qS([(num == null ? null : (function(k,v) {return encodeURIComponent(k)+'='+encodeURIComponent(v)})("num", num)), (offset == null ? null : (function(k,v) {return encodeURIComponent(k)+'='+encodeURIComponent(v)})("offset", offset))])})
      }

_nS('controllers.Courses'); _root.controllers.Courses.createCourse =
      function() {
      return _wA({method:"POST", url:"/" + "courses"})
      }

_nS('controllers.Courses'); _root.controllers.Courses.unapproved =
      function(num,offset) {
      return _wA({method:"GET", url:"/" + "courses/unapproved" + _qS([(num == null ? null : (function(k,v) {return encodeURIComponent(k)+'='+encodeURIComponent(v)})("num", num)), (offset == null ? null : (function(k,v) {return encodeURIComponent(k)+'='+encodeURIComponent(v)})("offset", offset))])})
      }

_nS('controllers.Courses'); _root.controllers.Courses.approve =
      function(id) {
      return _wA({method:"PUT", url:"/" + "courses/" + (function(k,v) {return v})("id", id) + "/approve"})
      }

_nS('controllers.Courses'); _root.controllers.Courses.info =
      function(id) {
      return _wA({method:"GET", url:"/" + "courses/" + (function(k,v) {return v})("id", id)})
      }

_nS('controllers.Courses'); _root.controllers.Courses.parseFile =
      function() {
      return _wA({method:"POST", url:"/" + "courses/parseFile"})
      }

_nS('controllers.Courses'); _root.controllers.Courses.list =
      function(num,offset) {
      return _wA({method:"GET", url:"/" + "courses" + _qS([(num == null ? null : (function(k,v) {return encodeURIComponent(k)+'='+encodeURIComponent(v)})("num", num)), (offset == null ? null : (function(k,v) {return encodeURIComponent(k)+'='+encodeURIComponent(v)})("offset", offset))])})
      }

_nS('controllers.Application'); _root.controllers.Application.jsRoutes =
      function() {
      return _wA({method:"GET", url:"/" + "jsroutes.js"})
      }

_nS('controllers.Application'); _root.controllers.Application.index =
      function() {
      return _wA({method:"GET", url:"/"})
      }

_nS('controllers.Images'); _root.controllers.Images.avatar =
      function(id) {
      return _wA({method:"GET", url:"/" + "image/avatar/" + (function(k,v) {return v})("id", id)})
      }

_nS('controllers.Images'); _root.controllers.Images.raw =
      function(id) {
      return _wA({method:"GET", url:"/" + "image/raw/" + (function(k,v) {return v})("id", id)})
      }

})(jsRoutes)
