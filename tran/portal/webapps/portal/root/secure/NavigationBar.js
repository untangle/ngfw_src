// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function NavigationBar(parent)
{
   if (0 == arguments.length) {
      return;
   }

   DwtToolBar.call(this, parent, "ToolBar", DwtControl.ABSOLUTE_STYLE, 2);

   this.home = new DwtButton(this);
   this.home.setImage("Home"); // XXX change to "Home"
   this.home.setToolTipContent("Portal Home");

   this.title = new DwtLabel(this, DwtLabel.ALIGN_LEFT, "NavBarTitle");

   this.bm = new DwtLabel(this, DwtLabel.ALIGN_LEFT, "NavBarBookmark");

   this.addFiller();

   this.logout = new DwtButton(this);
   this.logout.setImage("Logout");
   this.logout.setToolTipContent("Logout");

   this.portalMode();
}

NavigationBar.prototype = new DwtToolBar();
NavigationBar.prototype.constructor = NavigationBar;

// public methods -------------------------------------------------------------

NavigationBar.prototype.portalMode = function()
{
   this.title.setText("Metavize Portal");
   this.bm.setText("bookmark");
}

NavigationBar.prototype.applicationMode = function(bookmark)
{
   this.title.setText(bookmark.app);
   this.bm.setText(bookmark.name);
   this.bm.setToolTipContent(bookmark.target);
}

NavigationBar.prototype.addHomeButtonListener = function(l)
{
   this.home.addSelectionListener(l);
}

// listeners ------------------------------------------------------------------

