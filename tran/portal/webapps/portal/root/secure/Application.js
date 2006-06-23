// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function Application(name, description, isHostService, appJs)
{
   this.name = name;
   this.description = description;
   this.isHostService = isHostService;
   eval("this._appCode = " + appJs);
}

Application.prototype.toString = function()
{
   return "Application: " + this.name;
};

Application.prototype.openBookmark = function()
{
   this._appCode.openBookmark.apply(this, arguments);
}

Application.prototype.openApplication = function()
{
   this._appCode.openApplication.apply(this, arguments);
}
