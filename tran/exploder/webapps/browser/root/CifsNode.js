// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function CifsNode(name, size, lastModified) {
   this.name = name;
   this.size = size;
   this.lastModified = lastModified;
}

CifsNode.prototype = {
   toString: function() {
      return this.name;
   }
}