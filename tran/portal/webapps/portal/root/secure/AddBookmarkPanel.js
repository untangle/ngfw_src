// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function AddBookmarkPanel(parent, apps)
{
   if (0 == arguments.length) {
      return;
   }

   DwtComposite.call(this, parent);

   this._bookmarkWidgets = [ ];

   this._apps = [];
   for (var i = 0; i < apps.length; i++) {
      this._apps.push(new DwtSelectOption(apps[i], false, apps[i].name));
   }

   this._init();
}

AddBookmarkPanel.prototype = new DwtComposite();
AddBookmarkPanel.prototype.constructor = AddBookmarkPanel;

// public methods -------------------------------------------------------------

AddBookmarkPanel.prototype.getBookmark = function()
{
   var app = this._appField.getValue();
   var fn = app.bookmarkFunction;

   var target;
   if (fn) {
      target = fn(this._fields);
   } else {
      target = this._targetField.getValue();
   }

   return new Bookmark(null, this._nameField.getValue(),
                       this._appField.getValue(), target);
}

AddBookmarkPanel.prototype.focus = function()
{
   this._fields[0].focus();
}

// private fields -------------------------------------------------------------

AddBookmarkPanel._PROPS_KEY = "props";

// private methods ------------------------------------------------------------

AddBookmarkPanel.prototype._init = function()
{
   this._fields = new Array();

   var label = new DwtLabel(this);
   label.setText("Application:");
   this._appField = new DwtSelect(this, this._apps);
   var l = new AjxListener(this, this._showFields);
   this._appField.addChangeListener(l);
   this._fields.push(this._appField);

   this._showFields();
}

AddBookmarkPanel.prototype._showFields = function()
{
   var app = this._appField.getValue();
   var props = app.bookmarkProperties();
   if (props) {
      this._showPropFields(props);
   } else {
      this._showDefaultFields();
   }
}

AddBookmarkPanel.prototype._showDefaultFields = function()
{
   this._fields = this._fields.splice(0, 1);

   for (var i = 0; i < this._bookmarkWidgets.length; i++) {
      this.removeChild(this._bookmarkWidgets[i]);
   }

   var label = new DwtLabel(this);
   this._bookmarkWidgets.push(label);
   label.setText("Name:");
   this._nameField = new DwtInputField({ parent: this });
   this._bookmarkWidgets.push(this._nameField);
   this._fields.push(this._nameField);

   label = new DwtLabel(this);
   this._bookmarkWidgets.push(label);
   label.setText("Target:");
   this._targetField = new DwtInputField({ parent: this });
   this._bookmarkWidgets.push(this._targetField);
   this._fields.push(this._targetField);
}

AddBookmarkPanel.prototype._showPropFields = function(props)
{
   this._fields = this._fields.splice(0, 1);

   for (var i = 0; i < this._bookmarkWidgets.length; i++) {
      this.removeChild(this._bookmarkWidgets[i]);
   }

   for (var f in props) {
      var prop = props[f];
      var label = prop.getLabel(this);
      this._bookmarkWidgets.push(label);
      var field = prop.getField(this);
      field.setData(AddBookmarkPanel._PROPS_KEY, prop);
      this._fields.push(field);
      this._bookmarkWidgets.push(field);
   }
}
