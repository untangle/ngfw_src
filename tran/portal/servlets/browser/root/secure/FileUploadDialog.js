// Copyright (c) 2003-2007 Untangle, Inc.
// All rights reserved.

// -----------------------------------------------------------------------------
// FileUploadDialog
// -----------------------------------------------------------------------------

function FileUploadDialog(parent, url, dest)
{
   if (arguments.length == 0) {
      return;
   }

   var className = null; // XXX

   DwtDialog.call(this, parent, className, "Upload File");

   var panel = new FileUploadPanel(this, url, dest);

   var cb = function() {
      this.setButtonEnabled(DwtDialog.OK_BUTTON, false);
      panel.upload();
   }
   this.setButtonListener(DwtDialog.OK_BUTTON, new AjxListener(this, cb));

   panel.addUploadCompleteListener(new AjxListener(this, this._uploadCompleteListener));

   this.setView(panel);
}

FileUploadDialog.prototype = new DwtDialog();
FileUploadDialog.prototype.constructor = FileUploadDialog;

// public methods -------------------------------------------------------------

FileUploadDialog.prototype.addUploadCompleteListener = function(listener)
{
   this.addListener(FileUploadPanel.UPLOAD_COMPLETE, listener);
}

FileUploadDialog.prototype.removeUploadCompleteListener = function(listener)
{
   this.removeListener(FileUploadPanel.UPLOAD_COMPLETE, listener);
}

// internal methods -----------------------------------------------------------

FileUploadDialog.prototype._uploadCompleteListener = function(evt)
{
   evt.dialog = this;

   this.notifyListeners(FileUploadPanel.UPLOAD_COMPLETE, evt);
}

// -----------------------------------------------------------------------------
// FileUploadPanel
// -----------------------------------------------------------------------------

// constants ------------------------------------------------------------------

FileUploadPanel.UPLOAD_COMPLETE = "UPLOAD_COMPLETE";

// constructors ---------------------------------------------------------------

function FileUploadPanel(parent, url, dest)
{
   if (0 == arguments.length) {
      return;
   }

   DwtComposite.call(this, parent, "FileUploadPanel");

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

      iFrame.uploadFailure = function(msg) {
         // XXX
      }
   }

   this.form = document.createElement("form");
   this.form.action = url;
   this.form.method = "post";
   this.form.enctype = this.form.encoding = "multipart/form-data";
   this.form.target = id;

   var label = this.form.appendChild(document.createTextNode("Upload a file: "));

   var file = document.createElement("input");
   file.type = "file";
   file.name = "file";
   this.form.appendChild(file);

   var hidden = document.createElement("input");
   hidden.type = "hidden";
   hidden.name = "dest";
   hidden.value = dest;
   this.form.appendChild(hidden);

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
