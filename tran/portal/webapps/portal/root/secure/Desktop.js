// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function Desktop(parent, className, posStyle)
{
   if (0 == arguments.length) {
      return;
   }

   DwtComposite.call(this, parent, className, posStyle);

   this._init();
}

Desktop.prototype = new DwtComposite();
Desktop.prototype.constructor = Desktop;

// private methods ------------------------------------------------------------

Desktop.prototype._init = function()
{
}
