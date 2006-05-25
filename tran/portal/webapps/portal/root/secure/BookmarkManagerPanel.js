// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function BookmarkManagerPanel(parent)
{
   if (0 == arguments.length) {
      return;
   }

   DwtComposite.call(this, parent);

   this._init();
}

BookmarkManagerPanel.prototype = new DwtComposite();
BookmarkManagerPanel.prototype.constructor = BookmarkManagerPanel;

// public methods -------------------------------------------------------------


// private methods ------------------------------------------------------------

BookmarkManagerPanel.prototype._init = function()
{
   // set up panel contents here
   var l = new DwtLabel(this);
   l.setText("Hello World");
}
