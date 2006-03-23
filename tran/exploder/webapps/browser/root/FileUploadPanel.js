// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

// constants ------------------------------------------------------------------

FileUploadPanel.UPLOAD_COMPLETE = "UPLOAD_COMPLETE";

// constructors ---------------------------------------------------------------

function FileUploadPanel(parent, className, posStyle)
{
   if (0 == arguments.length) {
      return;
   }

   DwtComposite.call(this, parent, className || "FileUploadPanel", posStyle);

   var htmlElem = this.getHtmlElement();

   var id = Dwt.getNextId();
   var iFrame = this._makeHiddenIFrame(htmlElem, id);

   with (this) {
      iFrame.uploadComplete = function() {
         var nl = function() {
            var evt = { };
            notifyListeners(FileUploadPanel.UPLOAD_COMPLETE, evt);
         }

         setTimeout(nl, 1000);
      };
   }

   this.form = document.createElement("form");
   this.form.action = "put" // XXX make a param
   this.form.method = "post";
   this.form.enctype = this.form.encoding = "multipart/form-data";
   this.form.target = id;

   var label = this.form.appendChild(document.createTextNode("Upload a file: "));

   var file = document.createElement("input");
   file.type = "file";
   file.name = "file";
   this.form.appendChild(file);

   this.getHtmlElement().appendChild(this.form);
}

FileUploadPanel.prototype = new DwtComposite();
FileUploadPanel.prototype.constructor = FileUploadPanel;

// public methods -------------------------------------------------------------

FileUploadPanel.prototype.upload = function()
{
   this.form.submit();

   this.getHtmlElement().removeChild(this.form, DwtLabel.ALIGN_CENTER)
   var l = new DwtLabel(this);
   l.setText("Uploading...");
}

FileUploadPanel.prototype.addUploadCompleteListener = function(listener)
{
   this.addListener(FileUploadPanel.UPLOAD_COMPLETE, listener);
}

FileUploadPanel.prototype.removeUploadCompleteListener = function(listener)
{
   this.removeListener(FileUploadPanel.UPLOAD_COMPLETE, listener);
}

// internal methods -----------------------------------------------------------

FileUploadPanel.prototype._makeHiddenIFrame = function(parent, name)
{
   parent.innerHTML += '\<iframe id="' + name + '" name="' + name
      + '" style="display: none"><\/iframe>';

   var children = parent.childNodes;
   for (var i = 0; i < children.length; i++) {
      var c = children[i];
      if (c.id == name) {
         return c;
      }
   }
}
