// Copyright (c) 2003-2007 Untangle, Inc.
// All rights reserved.

function CifsNode(parent, name, principal, type, size, lastModified,
                  contentType, authorized)
{
    this.parent = parent;
    this.name = name;
    this.principal = principal;
    this.type = type;
    this.size = size || 0;
    this.contentType = contentType;
    this.authorized = authorized || true;

    if (0 < lastModified) {
        var date = new Date();
        date.setTime(lastModified);
        var formatter = AjxDateFormat.getDateTimeInstance(AjxDateFormat.MEDIUM, AjxDateFormat.SHORT);
        this.lastModified = formatter.format(date);
    } else {
        this.lastModified = "";
    }
    this.lastModifiedRaw = lastModified;  // Save for later comparison

    this.url = (parent || "") + name;

    if (this.name.length - 1 == this.name.lastIndexOf("/")) {
        this.label = this.name.substring(0, this.name.length - 1);
    } else {
        this.label = this.name;
    }

    this.label = this.label.replace(/\//g, "\\");
}

CifsNode.FILE = 1;
CifsNode.DIRECTORY = 128;

CifsNode.WORKGROUP = 2;
CifsNode.SERVER = 4;
CifsNode.SHARE = 8;
CifsNode.NAMED_PIPE = 16;
CifsNode.PRINTER = 32;
CifsNode.COMM = 64;

CifsNode.TYPES = { };
CifsNode.TYPES["file"] = CifsNode.FILE;
CifsNode.TYPES["directory"] = CifsNode.DIRECTORY;
CifsNode.TYPES["workgroup"] = CifsNode.WORKGROUP;
CifsNode.TYPES["server"] = CifsNode.SERVER;
CifsNode.TYPES["share"] = CifsNode.SHARE;
CifsNode.TYPES["named_pipe"] = CifsNode.NAMED_PIPE;
CifsNode.TYPES["printer"] = CifsNode.PRINTER;
CifsNode.TYPES["comm"] = CifsNode.COMM;

CifsNode.ICON_NAMES = { };
CifsNode.ICON_NAMES[CifsNode.WORKGROUP] = "WorkGroup";
CifsNode.ICON_NAMES[CifsNode.SERVER] = "Server";
CifsNode.ICON_NAMES[CifsNode.SHARE] = "Share";
CifsNode.ICON_NAMES[CifsNode.PRINTER] = "Printer";
CifsNode.ICON_NAMES[CifsNode.NAMED_PIPE] = "NamedPipe";
CifsNode.ICON_NAMES[CifsNode.COMM] = "Comm";

CifsNode.ICON_NAMES[CifsNode.FILE] = "File";
CifsNode.ICON_NAMES[CifsNode.DIRECTORY] = "Folder";

CifsNode.prototype = {
    toString: function() {
        return this.name;
    },

    getReqUrl: function() {
        return escape(null == this.principal ? this.url : ("[" + this.principal + "]" + this.url));
    },

    getPostUrl: function() {
        return null == this.principal ? this.url : ("[" + this.principal + "]" + this.url);
    },

    getFullPath: function() {
        return this.url.replace(/\//g, "\\");
    },

    isWorkGroup: function() {
        return this.type == CifsNode.WORKGROUP;
    },

    isServer: function() {
        return this.type == CifsNode.SERVER;
    },

    isShare: function() {
        return this.type == CifsNode.SHARE;
    },

    isDirectory: function() {
        return this.type == CifsNode.DIRECTORY;
    },

    getIconName: function() {
        return CifsNode.ICON_NAMES[this.type]
        + (this.authorized ? "" : "NoAuth");
    },

    getDomain: function() {
        var i = this.url.indexOf('/', 2);
        return i < 2 ? this.url : this.url.substring(2, i);
    }
}
