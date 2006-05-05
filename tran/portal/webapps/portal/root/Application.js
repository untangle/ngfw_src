// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function Application(name)
{
   this.name = name;
}

Application.prototype = {
   toString: function() {
      return this.name;
   }
}