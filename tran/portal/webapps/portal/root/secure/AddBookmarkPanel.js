// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function AddBookmarkPanel(parent, apps)
{
   if (0 == arguments.length) {
      return;
   }

   DwtComposite.call(this, parent);


   this._apps = new Array();
   for (var i = 0; i < apps.length; i++) {
      DBG.println("ADDED: " + apps[i].name);
      this._apps.push(apps[i].name);
   }

   this._init();
}

AddBookmarkPanel.prototype = new DwtComposite();
AddBookmarkPanel.prototype.constructor = AddBookmarkPanel;

// public methods -------------------------------------------------------------

AddBookmarkPanel.prototype.getBookmark = function()
{
   return new Bookmark(null, this._nameField.getValue(),
                         this._appField.getValue(),
                         this._targetField.getValue());
}

AddBookmarkPanel.prototype.focus = function()
{
   this._fields[0].focus();
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
   label.setText("Application:");
   this._appField = new DwtSelect(this, this._apps);
   this._fields.push(this._appField);

   label = new DwtLabel(this);
   label.setText("Target:");
   this._targetField = new DwtInputField({ parent: this });
   this._fields.push(this._targetField);
}
