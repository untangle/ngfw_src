// Copyright (c) 2003-2007 Untangle, Inc.
// All rights reserved.

function BookmarkProperty(id, name, values, def, required, valFn)
{
    this.id = id;
    this.name = name;
    this.values = values;
    this.def = def;
    this.required = true == required;
    this.valFn = valFn;
};

BookmarkProperty.prototype.getField = function(parent, val)
{
    var f;

    if (null == this.values) {
        f = new DwtInputField({ parent: parent, validator: this.valFn });
        if (val) {
            f.setValue(val);
        }
        f.setRequired(this.required);
    } else if (this.values instanceof Array) {
        f = new DwtSelect(parent, this.values);
        if (val) {
            f.setSelectedValue(val);
        } else if (this.def) {
            f.setSelectedValue(this.def);
        }
    } else {
        f = new DwtInputField({ parent: parent, validator: this.valFn });
        if (val) {
            f.setValue(val);
        } else {
            f.setValue(this.values);
        }
        f.setRequired(this.required);
    }

    return f;
};
