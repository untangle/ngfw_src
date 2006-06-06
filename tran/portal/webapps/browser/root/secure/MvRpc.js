// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

MvRpc = { };

MvRpc.invoke = function(requestStr, serverUrl, requestHeaders, useGet,
                        actionCallback, timeoutCallback, authCallback)
{
   DBG.println("MvRpc.invoke");

   var obj = {
      actionCallback: actionCallback,
      timeoutCallback: timeoutCallback,
      authCallback: authCallback
   };

   var cb = new AjxCallback(this, MvRpc._callbackFn, obj);

   AjxRpc.invoke(requestStr, serverUrl, requestHeaders, cb, useGet);
}

// public methods -------------------------------------------------------------

MvRpc._reloadPage = function()
{
   window.location.reload();
}

MvRpc.reloadPageCallback = new AjxCallback(null, MvRpc._reloadPage, { });

// private methods ------------------------------------------------------------

MvRpc._callbackFn = function(obj, results)
{
   if (results.xml) {
      DBG.println("first child: " + results.xml.firstChild.tagName);
      if (obj.authCallback && "error" == results.xml.firstChild.tagName) {
         DBG.println("RUNNING authCallback");
         return obj.authCallback.run(results);
      } else if (obj.actionCallback) {
         DBG.println("RUNNING actionCallback");
         return obj.actionCallback.run(results);
      }
   } else {
      // XXX detect timeout page
      if (obj.timeoutCallback) {
         return obj.timeoutCallback.run(results);
         DBG.println("RUNNING timeoutCallback");
      } else {
         DBG.println("DOING NOTHING");
      }
   }
}