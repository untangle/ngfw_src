// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function AddBookmarkPanel(parent)
{
   if (0 == arguments.length) {
      return;
   }

   DwtComposite.call(this, parent);

   this._init();
}

AddBookmarkPanel.prototype = new DwtComposite();
AddBookmarkPanel.prototype.constructor = AddBookmarkPanel;

// public methods -------------------------------------------------------------

AddBookmarkPanel.prototype.getBookmark = function()
{
   return new Bookmark(this._nameField.getValue(), this._appField.getValue(),
                       this._targetField.getValue());
}

// private methods ------------------------------------------------------------

AddBookmarkPanel.prototype._init = function()
{
   this._fields = new Array();

   var label = new DwtLabel(this);
   label.setText("Name:");
   this._nameField = new DwtInputField({ parent: this });
   this._fields.push(this._nameField);

   label = new DwtLabel(this);
   label.setText("Application:"); // XXX dropdown
   this._appField = new DwtInputField({ parent: this });
   this._fields.push(this._appField);

   label = new DwtLabel(this);
   label.setText("Target:"); // XXX dropdown
   this._targetField = new DwtInputField({ parent: this });
   this._fields.push(this._targetField);
}
