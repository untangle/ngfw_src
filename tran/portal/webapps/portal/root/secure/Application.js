// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function Application(name, appJs)
{
   this.name = name;
   eval("this._appCode = " + appJs);
}

Application.prototype.toString = function() {
   return this.name;
};

Application.prototype.openBookmark = function() {
   this._appCode.openBookmark.apply(this, arguments);
}
