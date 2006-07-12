// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function Application(name, description, isHostService, appJsUrl)
{
  this.name = name;
  this.description = description;
  this.isHostService = isHostService;
  this.loaded = false;

  var cb = new AjxCallback(this, this._loadAppJs, { });
  AjxRpc.invoke(null, appJsUrl, null, cb, { }, true);
};

Application.prototype.toString = function()
{
  return "Application: " + this.name;
};

Application.prototype.getBookmarkProperties = function()
{
  return this._appCode ? this._appCode.bookmarkProperties : null;
};

Application.prototype.getBookmarkFunction = function()
{
  return this._appCode ? this._appCode.bookmarkFunction : null;
};

Application.prototype.openBookmark = function()
{
  return this._appCode ? this._appCode.openBookmark.apply(this, arguments) : null;
};

Application.prototype.openApplication = function()
{
  return this._appCode ? this._appCode.openApplication.apply(this, arguments) : null;
};

// private functions ----------------------------------------------------------

Application.prototype._loadAppJs = function(obj, results)
{
  if (results.text) {
    DBG.println("APP CODE TEXT: " + results.text);
    eval("this._appCode = " + results.text);
    DBG.println("SET _appCode: " + this._appCode);
  } else {
    DBG.println("COULD NOT SET _appCode: " + results);
  }
  this.loaded = true;
};
