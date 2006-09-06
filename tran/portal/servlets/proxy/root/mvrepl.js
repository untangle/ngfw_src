// Copyright (c) 2006 Metavize Inc.
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
}
