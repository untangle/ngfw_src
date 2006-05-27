// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function AppWindow(parent, url)
{
   if (arguments.length == 0) {
      return;
   }

   DwtDialog.call(this, parent, null, "TITLE HERE", null, null, null,
                  DwtBaseDialog.MODELESS);

   this.initializeResizing();

   DBG.println("USING URL: " + url);
   this.setContent("<iframe src='" + url + "'></iframe>");
}

AppWindow.prototype = new DwtDialog();
AppWindow.prototype.constructor = AppWindow;

// public methods -------------------------------------------------------------

// internal methods -----------------------------------------------------------

AppWindow.prototype.initializeResizing = function()
{
   DBG.println(this.getHtmlElement());
}
