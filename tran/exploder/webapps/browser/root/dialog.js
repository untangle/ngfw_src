// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

// Dialog

function testDialog()
{
   var dialog = new Dialog("Test Dialog", document.createTextNode("HOLY CRAP"));
   dialog.setVisible(true);
}

function Dialog(title, content)
{
   this.div = document.createElement("div");

   var s = this.div.appendChild(document.createElement("div"));
   Element.addClassName(s, "shadow");

   var md = s.appendChild(document.createElement("div"));
   Element.addClassName(md, "dialog");

   var titlebar = new TitleBar(md, title);
   with (this) {
      titlebar.onClose = function() { setVisible(false); };
   }

   md.appendChild(content);

   this.clickBlocker = new ClickBlocker();
}

Dialog.prototype.setVisible = function(visible)
{
   if (visible != this.visible) {
      if (visible) {
         var b = document.getElementsByTagName("body")[0];
         b.appendChild(this.div);
      } else {
         this.div.parentNode.removeChild(this.div);
      }

      this.clickBlocker.setVisible(visible);
   }

   this.visible = visible;
}

// TitleBar

function TitleBar(parent, title)
{
   this.parent = parent;

   var elem = parent.appendChild(document.createElement("table"));
   Element.addClassName(elem, "titlebar");

   var tbody = elem.appendChild(document.createElement("tbody"));

   var tr = tbody.appendChild(document.createElement("tr"));

   var td = tr.appendChild(document.createElement("td"));
   this.titleText = td.appendChild(document.createTextNode(title));

   td = tr.appendChild(document.createElement("td"));
   Element.addClassName(td, "titlebar-close");

   var close = td.appendChild(document.createElement("img"));
   close.src = "close.gif";
   with (this) {
      close.onclick = function() { onClose(); };
   }
}

TitleBar.prototype.setTitle = function(title)
{
   this.titleText.textContent = title;
}

TitleBar.prototype.onClose = function() { alert("OH NO!"); }

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

