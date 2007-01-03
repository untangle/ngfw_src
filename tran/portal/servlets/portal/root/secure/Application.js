// Copyright (c) 2003-2007 Untangle, Inc.
// All rights reserved.

function Application(name, description, longDescription, isHostService,
                     appJsUrl, loadedCb)
{
    this.name = name;
    this.description = description;
    this.longDescription = longDescription;
    this.isHostService = isHostService;
    this.loadedCb = loadedCb;
    this.loaded = false;

    var cb = new AjxCallback(this, this._loadAppJs, { });
    AjxRpc.invoke(null, appJsUrl, null, cb, { }, true);
};

Application.prototype.toString = function()
{
    return "Application: " + this.name + "[code: " + this._appCode + "]";
};

Application.prototype.getBookmarkProperties = function()
{
    return this._appCode ? this._appCode.bookmarkProperties : null;
};

Application.prototype.getBookmarkFunction = function()
{
    return this._appCode ? this._appCode.bookmarkFunction : null;
};

Application.prototype.getPropertiesFunction = function()
{
    return this._appCode ? this._appCode.propertiesFunction : null;
};

Application.prototype.getTooltipFunction = function()
{
    return this._appCode ? this._appCode.tooltipFunction : null;
};

Application.prototype.getTargetExample = function()
{
    return this._appCode ? this._appCode.targetExample : null;
};

Application.prototype.isBookmarkable = function()
{
    return this._appCode ? (this._appCode.openBookmark != undefined) : false;
};

Application.prototype.openBookmark = function()
{
    return this._appCode ? this._appCode.openBookmark.apply(this, arguments) : null;
};

Application.prototype.openApplication = function()
{
    return this._appCode ? this._appCode.openApplication.apply(this, arguments) : null;
};

Application.prototype.getIconUrl = function()
{
    return this._appCode ? this._appCode.iconUrl : null;
}

// private functions ----------------------------------------------------------

    Application.prototype._loadAppJs = function(obj, results)
{
    if (results.text) {
        eval("this._appCode = " + results.text);
    } else {
        DBG.println("COULD NOT SET _appCode: " + results);
    }
    this.loaded = true;

    if (this.loadedCb) {
        this.loadedCb.run(this);
    }
};
