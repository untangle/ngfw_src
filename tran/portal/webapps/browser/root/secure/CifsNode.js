// Copyright (c) 2006 Metavize Inc.
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

   this.url = (parent || "") + name;

   if (this.name.length - 1 == this.name.lastIndexOf("/")) {
      this.label = this.name.substring(0, this.name.length - 1);
   } else {
      this.label = this.name;
   }
}

CifsNode.WORKGROUP = "workgroup";
CifsNode.SERVER = "server";
CifsNode.SHARE = "share";
CifsNode.FILE = "file";
CifsNode.DIR = "dir";

CifsNode.ICON_NAMES = { };
CifsNode.ICON_NAMES[CifsNode.WORKGROUP] = "WorkGroup";
CifsNode.ICON_NAMES[CifsNode.SERVER] = "Server";
CifsNode.ICON_NAMES[CifsNode.SHARE] = "Share";
CifsNode.ICON_NAMES[CifsNode.FILE] = "File";
CifsNode.ICON_NAMES[CifsNode.DIR] = "Folder";

CifsNode.prototype = {
   toString: function() {
      return this.name;
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
      return this.type == CifsNode.DIR;
   },

   getIconName: function() {
      return CifsNode.ICON_NAMES[this.type]
         + (this.authorized ? "" : "NoAuth");
   }
}
