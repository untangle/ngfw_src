// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function uploadFile()
{
   var form = document.createElement("form");
   form.action = "toolbar.jsp"
   form.method = "post";
   form.enctype = "multipart/form-data";
   form.target = "hidden-target";

   var iframe = form.appendChild(document.createElement("iframe"));
   iframe.name = "hidden-target";

   var input = form.appendChild(document.createElement("input"));
   input.type = "file"
   input.name = "file";

   input = form.appendChild(document.createElement("input"));
   input.type = "submit";
   input.value = "Upload";

   return modalDialog("Upload File", form);
}

function modalDialog(title, content)
{
   var s = document.createElement("div");
   Element.addClassName(s, "shadow");

   var md = s.appendChild(document.createElement("div"));
   Element.addClassName(md, "dialog");

   var tb = md.appendChild(document.createElement("div"));
   Element.addClassName(tb, "dialog-titlebar");
   tb.appendChild(document.createTextNode(title));

   md.appendChild(content);

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