// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function Application(name, appJs)
{
   this.name = name;
   this._appCode = eval(appJs);
   DBG.println("APPCODE: " + this._appCode);
}

Application.prototype.toString = function() {
   return this.name;
};

Application.prototype.openBookmark = function(target) {
   this._appCode.openBookmark(target);
}
