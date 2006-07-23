// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function Bookmark(id, name, app, target) {
    this.id = id;
    this.name = name;
    this.app = app;
    this.target = target;
};

Bookmark.prototype = {
    toString: function() {
        return "Bookmark [id: " + this.id + " name: " + this.name
        + " app: " + this.app + " target: " + this.target + "]";
    }
};

// XXX naive implementation
Bookmark.prototype.splitTarget = function()
{
    var obj = new Object();

    var fields = this.target.split("&");
    for (var i = 0; i < fields.length; i++) {
        var p = fields[i].split("=");
        DBG.println("SPLIT: " + p[0] + " = " + p[1]);
        obj[p[0]] = p[1];
    }

    return obj;
}