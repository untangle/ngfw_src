// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function ApplicationIframe(parent, url)
{
   if (0 == arguments.length) {
      return;
   }

   DwtTabViewPage.call(this, parent, "ApplicationIframe", DwtControl.ABSOLUTE_STYLE);

   this.setContent("<iframe class='kaka' src='" + url + "'></iframe>");
}

ApplicationIframe.prototype = new DwtTabViewPage();
ApplicationIframe.prototype.constructor = ApplicationIframe;
