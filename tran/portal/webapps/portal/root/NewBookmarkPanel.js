// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function NewBookmarkPanel(parent)
{
   if (0 == arguments.length) {
      return;
   }

   DwtComposite.call(this, parent);

   this._draw();
}

NewBookmarkPanel.prototype = new DwtComposite();
NewBookmarkPanel.prototype.constructor = NewBookmarkPanel;

// public methods -------------------------------------------------------------

NewBookmarkPanel.prototype.getBookmark = function()
{
   return new Bookmark(this._nameField.getValue(), this._appField.getValue(),
                       this._targetField.getValue());
}

// private methods ------------------------------------------------------------

NewBookmarkPanel.prototype._draw = function()
{
   this.clear();

   var label = new DwtLabel(this);
   label.setText("Name:");
   this._nameField = new DwtInputField({ parent: this });

   label = new DwtLabel(this);
   label.setText("Application:"); // XXX dropdown
   this._appField = new DwtInputField({ parent: this });

   label = new DwtLabel(this);
   label.setText("Target:"); // XXX dropdown
   this._targetField = new DwtInputField({ parent: this });
}
