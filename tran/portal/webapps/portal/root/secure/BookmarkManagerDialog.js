// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function BookmarkManagerDialog(parent)
{
   if (arguments.length == 0) {
      return;
   }

   DwtDialog.call(this, parent, null, "Bookmark Manager");

   this._panel = new BookmarkManagerPanel(this);
   this.addListener(DwtEvent.ONFOCUS, new AjxListener(this, this._focusListener));

   this.setView(this._panel);
}

BookmarkManagerDialog.prototype = new DwtDialog();
BookmarkManagerDialog.prototype.constructor = BookmarkManagerDialog;

// public methods -------------------------------------------------------------

// internal methods -----------------------------------------------------------

BookmarkManagerDialog.prototype._focusListener = function(ev)
{
   this._panel.focus();
}
