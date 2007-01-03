// Copyright (c) 2003-2007 Untangle, Inc.
// All rights reserved.

function Bookmark(id, name, app, target, type) {
    this.id = id;
    this.name = name;
    this.app = app;
    this.target = target;
    this.type = type;
};

Bookmark.prototype = {
    toString: function() {
        return "Bookmark [id: " + this.id + " name: " + this.name
        + " app: " + this.app + " target: " + this.target +
        + " type: " + this.type + "]";
    }
};

Bookmark.USER_TYPE = "user";

// XXX naive implementation
Bookmark.prototype.splitTarget = function()
{
    var obj = new Object();

    var fields = this.target.split("&");
    for (var i = 0; i < fields.length; i++) {
        var p = fields[i].split("=");
        obj[p[0]] = p[1];
    }

    return obj;
}