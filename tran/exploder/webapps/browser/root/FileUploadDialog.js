// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function FileUploadDialog(parent) {
   if (arguments.length == 0) {
      return;
   }

   var className = null; // XXX

   DwtDialog.call(this, parent, className, "Upload File");

   var form = this._createView();
   this.setView(form);

   form.draw();
}

FileUploadDialog.prototype = new DwtDialog();
FileUploadDialog.prototype.constructor = FileUploadDialog;

// internal methods -----------------------------------------------------------

FileUploadDialog.prototype._createView = function()
{
   var form = {
      id: "uploadForm",
      items: [{ type: _OUTPUT_, value: "Select file to upload:" },
              { type: _FILE_, value: "HELLO" },
              { ref: "name"  },
              { ref: "phone" }],
      defaultItemType: _TEXTFIELD_,
      itemDefaults: { _TEXTFIELD_: { width: 200 } }
   };

   return new XForm(form, null, null, this);
}
