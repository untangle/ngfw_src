// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

CifsNode.WORKGROUP = "workgroup"
CifsNode.FILE = "file"
CifsNode.DIR = "dir"

function CifsNode(parent, name, principal, type, size, lastModified,
                  contentType) {
   this.parent = parent;
   this.name = name;
   this.principal = principal;
   this.type = type;
   this.size = size || 0;
   this.contentType = contentType;

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

CifsNode.prototype = {
   toString: function() {
      return this.name;
   },

   isDirectory: function() {
      return this.type == CifsNode.DIR;
   }
}
