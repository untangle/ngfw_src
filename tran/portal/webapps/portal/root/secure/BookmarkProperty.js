// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function BookmarkProperty(id, name, values, def, valFn)
{
   this.id = id;
   this.name = name;
   this.values = values;
   this.def = def;
   this.valFn = valFn;
}

BookmarkProperty.prototype.getLabel = function(parent)
{
   var label = new DwtLabel(parent);
   label.setText(this.name + ":");
   return label;
}

BookmarkProperty.prototype.getField = function(parent)
{
   var f;

   if (null == this.values) {
      f = new DwtInputField({ parent: parent});
   } else if (this.values instanceof Array) {
      f = new DwtSelect(parent, this.values);
   } else {
      f = new DwtInputField({ parent: parent});
      f.setValue(this.values);
   }

   return f;
}