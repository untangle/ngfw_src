// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function Bookmark(id, name, app, target) {
   this.id = id;
   this.name = name;
   this.app = app;
   this.target = target;
}

Bookmark.prototype = {
   toString: function() {
      return "Bookmark [id: " + this.id + " name: " + this.name
         + " app: " + this.app + " target: " + this.target + "]";
   }
}
