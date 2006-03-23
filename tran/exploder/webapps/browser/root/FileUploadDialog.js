// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function FileUploadDialog(parent)
{
   if (arguments.length == 0) {
      return;
   }

   var className = null; // XXX

   DwtDialog.call(this, parent, className, "Upload File");

   var panel = new FileUploadPanel(this);

   var cb = function() {
      this.setButtonEnabled(DwtDialog.OK_BUTTON, false);
      panel.upload();
   }
   this.setButtonListener(DwtDialog.OK_BUTTON, new AjxListener(this, cb));

   var ul = function() { this.popdown(); }
   panel.addUploadCompleteListener(new AjxListener(this, ul));

   this.setView(panel);
}

FileUploadDialog.prototype = new DwtDialog();
FileUploadDialog.prototype.constructor = FileUploadDialog;
