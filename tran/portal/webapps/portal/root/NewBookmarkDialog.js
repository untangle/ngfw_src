// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function NewBookmarkDialog(parent)
{
   if (arguments.length == 0) {
      return;
   }

   var className = null; // XXX

   DwtDialog.call(this, parent, className, "NewBookmark");

   this._panel = new NewBookmarkPanel(this);
   this.addListener(DwtEvent.ONFOCUS, new AjxListener(this, this._focusListener));

   this.setView(this._panel);
}

NewBookmarkDialog.prototype = new DwtDialog();
NewBookmarkDialog.prototype.constructor = NewBookmarkDialog;

// public methods -------------------------------------------------------------

NewBookmarkDialog.prototype.getBookmark = function()
{
   return this._panel.getBookmark();
}

// internal methods -----------------------------------------------------------

NewBookmarkDialog.prototype._uploadCompleteListener = function(evt)
{
   evt.dialog = this;

   this.notifyListeners(FileUploadPanel.UPLOAD_COMPLETE, evt);
}

NewBookmarkDialog.prototype._focusListener = function(ev)
{
   this._panel.focus();
}
