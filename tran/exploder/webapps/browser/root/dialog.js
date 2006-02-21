// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function modalDialog(title)
{
   var s = document.createElement("div");
   Element.addClassName(s, "shadow");

   var md = s.appendChild(document.createElement("div"));
   Element.addClassName(md, "dialog");

   var tb = md.appendChild(document.createElement("div"));
   Element.addClassName(tb, "dialog-titlebar");
   tb.appendChild(document.createTextNode(title));


   var cb = clickBlocker();

   var b = document.getElementsByTagName("body")[0];
   b.appendChild(cb);
   b.appendChild(s);
}

function clickBlocker()
{
   var cb = document.createElement("div");
   cb.id = "click-blocker";

   return cb;
}