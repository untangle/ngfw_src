// Copyright (c) 2003-2007 Untangle, Inc.
// All rights reserved.

function mv_repl(url)
{
   var proto;
   var host;
   var path;

   if (0 == url.indexOf("//")) {
      var i = url.indexOf("/", 2);
      host = url.substring(2, i);
      path = url.substring(i);

      return "//" + mv_localHost + mv_contextBase + mv_proto + "/" + host + path;
   } else if (0 == url.indexOf("/")) {
      return mv_contextBase + mv_proto + mv_host + url;
   } else {
      var i = url.indexOf(":");
      if (0 > i) {
         return url;
      } else {
         proto = url.substring(0, i);
         i = i + 3;
         var j = url.indexOf('/', i);
         host = url.substring(i, j);
         path = url.substring(j);

         return "//" + mv_localHost + mv_contextBase + proto + "/" + host + path;
      }
   }
};

if (undefined == window.ut_windowOpen) {
    window.ut_windowOpen = window.open;
}

window.open = function(url, name, features, replace)
{
    window.ut_windowOpen(mv_repl(url), name, features, replace);
}
