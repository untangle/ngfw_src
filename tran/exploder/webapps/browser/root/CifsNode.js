// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function CifsNode(name) {
   this.name = name;
   this.size = 100;
   this.lastModified = "Yesterday";
}

CifsNode.prototype = {
   toString: function() {
      return this.name;
   }
}