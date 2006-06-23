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

Application.prototype.bookmarkProperties = function()
{
   return this._appCode.bookmarkProperties;
}

Application.prototype.openBookmark = function()
{
   return this._appCode.openBookmark.apply(this, arguments);
}

Application.prototype.openApplication = function()
{
   return this._appCode.openApplication.apply(this, arguments);
}
