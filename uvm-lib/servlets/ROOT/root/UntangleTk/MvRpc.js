// Copyright (c) 2003-2007 Untangle, Inc.
// All rights reserved.

MvRpc = { };

MvRpc.invoke = function(requestStr, serverUrl, requestHeaders, useGet,
                        actionCallback, timeoutCallback, authCallback)
{
    DBG.println("MvRpc.invoke(" + requestStr + ", " + serverUrl + ")");

    var obj = {
        actionCallback: actionCallback,
        timeoutCallback: timeoutCallback,
        authCallback: authCallback
    };

    var cb = new AjxCallback(this, MvRpc._callbackFn, obj);

    AjxRpc.invoke(requestStr, serverUrl, requestHeaders, cb, useGet);
};

// public methods -------------------------------------------------------------

MvRpc._reloadPage = function()
{
    window.location.reload();
};

MvRpc.reloadPageCallback = new AjxCallback(null, MvRpc._reloadPage, { });

// private methods ------------------------------------------------------------

MvRpc.MAGIC_RE = /<!-- MagicComment: MVTimeout -->/;

MvRpc._callbackFn = function(obj, results)
{
    if (results.xml) {
        var errors = results.xml.getElementsByTagName("auth-error");
        if (obj.authCallback && null != errors && 0 < errors.length) {
            return obj.authCallback.run(results);
        } else if (obj.actionCallback) {
            return obj.actionCallback.run(results);
        }
    } else if (obj.timeoutCallback && results.text && 0 <= results.text.search(MvRpc.MAGIC_RE)) {
        return obj.timeoutCallback.run(results);
    } else if (obj.actionCallback) {
        return obj.actionCallback.run(results);
    } else {
    }
};