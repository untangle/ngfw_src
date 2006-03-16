// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function CifsNode(name, size, lastModified) {
   this.name = name;
   this.size = size;
   if (0 < lastModified) {
      var date = new Date();
      date.setTime(lastModified);
      var formatter = AjxDateFormat.getDateTimeInstance(AjxDateFormat.MEDIUM, AjxDateFormat.SHORT);
      this.lastModified = formatter.format(date);
   } else {
      this.lastModified = "";
   }
}

CifsNode.prototype = {
   toString: function() {
      return this.name;
   }
}