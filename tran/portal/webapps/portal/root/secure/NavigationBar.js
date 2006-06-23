// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function NavigationBar(parent)
{
   if (0 == arguments.length) {
      return;
   }

   DwtToolBar.call(this, parent, "HorizontalToolBar", DwtControl.ABSOLUTE_STYLE, 2);

   this._home = new DwtButton(this);
   this._home.setImage("Home"); // XXX change to "Home"
   this._home.setToolTipContent("Portal Home");

   this.title = new DwtLabel(this, DwtLabel.ALIGN_LEFT, "NavBarTitle");

   this.bm = new DwtLabel(this, DwtLabel.ALIGN_LEFT, "NavBarBookmark");

   this.addFiller();

   this._logout = new DwtButton(this);
   this._logout.setImage("Logout");
   this._logout.setToolTipContent("Logout");

   this.portalMode();
}

NavigationBar.prototype = new DwtToolBar();
NavigationBar.prototype.constructor = NavigationBar;

// public methods -------------------------------------------------------------

NavigationBar.prototype.portalMode = function()
{
   this.title.setText("Metavize Portal");
   this.bm.setText("");
}

NavigationBar.prototype.applicationMode = function(application, bookmark)
{
   this.title.setText(application.description);
   if (bookmark) {
      this.bm.setText(bookmark.name);
      this.bm.setToolTipContent(bookmark.target);
   }
}

NavigationBar.prototype.addHomeButtonListener = function(l)
{
   this._home.addSelectionListener(l);
}


NavigationBar.prototype.addLogoutButtonListener = function(l)
{
   this._logout.addSelectionListener(l);
}

// listeners ------------------------------------------------------------------

