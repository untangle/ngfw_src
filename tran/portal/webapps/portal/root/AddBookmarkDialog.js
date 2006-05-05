// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function AddBookmarkDialog(parent, apps)
{
   if (arguments.length == 0) {
      return;
   }

   var className = null; // XXX

   DwtDialog.call(this, parent, className, "Add Bookmark");

   this._panel = new AddBookmarkPanel(this, apps);
   this.addListener(DwtEvent.ONFOCUS, new AjxListener(this, this._focusListener));

   this.setView(this._panel);
   this.setTabOrder(this._panel._fields);
}

AddBookmarkDialog.prototype = new DwtDialog();
AddBookmarkDialog.prototype.constructor = AddBookmarkDialog;

// public methods -------------------------------------------------------------

AddBookmarkDialog.prototype.getBookmark = function()
{
   return this._panel.getBookmark();
}

// internal methods -----------------------------------------------------------

AddBookmarkDialog.prototype._uploadCompleteListener = function(evt)
{
   evt.dialog = this;

   this.notifyListeners(FileUploadPanel.UPLOAD_COMPLETE, evt);
}

AddBookmarkDialog.prototype._focusListener = function(ev)
{
   this._panel.focus();
}
