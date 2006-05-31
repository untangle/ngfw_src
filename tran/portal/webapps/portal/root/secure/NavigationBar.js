// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function NavigationBar(parent)
{
   if (0 == arguments.length) {
      return;
   }

   DwtToolBar.call(this, parent, "Toolbar", DwtControl.ABSOLUTE_STYLE);

   this.portalMode();
}

NavigationBar.prototype = new DwtToolBar();
NavigationBar.prototype.constructor = NavigationBar;

// public methods -------------------------------------------------------------

NavigationBar.prototype.portalMode = function()
{
   this.removeChildren();
   var label = new DwtLabel(this);
   label.setImage("Home"); // XXX change to "Home"

   var title = new DwtLabel(this, DwtLabel.ALIGN_LEFT, "NavBarTitle");
   title.setText("Metavize Portal");

   var bm = new DwtLabel(this, DwtLabel.ALIGN_LEFT, "NavBarBookmark");
   bm.setText("bookmark");
}

NavigationBar.prototype.applicationMode = function(bookmark)
{
}
