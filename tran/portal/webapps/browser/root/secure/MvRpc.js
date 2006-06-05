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

/* MvRpc.reloadPageCallback = new AjxCallback(null, new function() { */
/*    DBG.println("MvRpc.reloadPageCallback"); */
/*    window.location.reload(); */
/* }); */

// private methods ------------------------------------------------------------

MvRpc._callbackFn = function(obj, results)
{
   if (results.xml) {
      if (obj.authCallback && "error" == results.xml.firstChild.tagName) {
         DBG.println("RUNNING authCallback");
         obj.authCallback.run(results);
      } else if (obj.actionCallback) {
         DBG.println("RUNNING actionCallback");
         obj.actionCallback.run(results);
      }
   } else {
      // XXX detect timeout page
      if (obj.timeoutCallback) {
         obj.timeoutCallback.run(results);
         DBG.println("RUNNING timeoutCallback");
      } else {
         DBG.println("DOING NOTHING");
      }
   }
}