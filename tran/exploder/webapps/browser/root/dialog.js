// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

// Dialog

function testDialog()
{
   var dialog = new Dialog("Test Dialog");
   dialog.setVisible(true);
}

function Dialog(title)
{
   this.div = document.createElement("div");

   var s = this.div.appendChild(document.createElement("div"));
   Element.addClassName(s, "shadow");

   var md = s.appendChild(document.createElement("div"));
   Element.addClassName(md, "dialog");

   var titlebar = md.appendChild(document.createElement("div"));
   Element.addClassName(titlebar, "dialog-titlebar");
   titlebar.appendChild(document.createTextNode(title));
   var exit = titlebar.appendChild(document.createElement("div"));
   Element.addClassName(exit, "dialog-close");

   //md.appendChild(content);
   md.appendChild(document.createTextNode("HOLY CRAP"));

   this.clickBlocker = new ClickBlocker();
}

Dialog.prototype.setVisible = function(visible)
{
   if (visible != this.visible) {
      if (visible) {
         var b = document.getElementsByTagName("body")[0];
         b.appendChild(this.div);
      } else {
         this.div.parentNode.removeChild(dialog);
      }

      this.clickBlocker.setVisible(visible);
   }

   this.visible = visible;
}


// ClickBlocker

function ClickBlocker()
{
   this.div = document.createElement("div");
   this.div.id = "click-blocker";
   this.visible = false;
}

ClickBlocker.prototype.setVisible = function(visible)
{
   if (visible != this.visible) {
      if (visible) {
         var b = document.getElementsByTagName("body")[0];
         b.appendChild(this.div);
      } else {
         this.div.parentNode.removeChild(this.div);
      }
   }

   this.visible = visible;
}

// Old

function uploadFile(url)
{
   var form = document.createElement("form");
   form.action = "put"
   form.method = "post";
   form.enctype = "multipart/form-data";
   form.target = "hidden-target";

   var iframe = form.appendChild(document.createElement("iframe"));
   iframe.name = "hidden-target";

   var file = form.appendChild(document.createElement("input"));
   file.type = "file"
   file.name = "file";

   input = form.appendChild(document.createElement("input"));
   input.type = "button";
   input.value = "Upload";
   input.onclick = function() { alert("KAKA: " + file.value); dialog.close(); }

   var dialog = modalDialog("Upload File", form);
}

