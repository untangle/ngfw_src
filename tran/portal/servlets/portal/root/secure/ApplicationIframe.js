// Copyright (c) 2003-2007 Untangle, Inc.
// All rights reserved.

function ApplicationIframe(parent)
{
   if (0 == arguments.length) {
      return;
   }

   DwtComposite.call(this, parent, "ApplicationIframe", DwtControl.STATIC_STYLE);
}

ApplicationIframe.prototype = new DwtComposite();
ApplicationIframe.prototype.constructor = ApplicationIframe;

// public methods -------------------------------------------------------------

ApplicationIframe.prototype.loadUrl = function(url)
{
   this.setContent("<iframe src='" + url + "'></iframe>");
}