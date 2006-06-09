// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function PortalPanel(parent)
{
   if (0 == arguments.length) {
      return;
   }

   DwtComposite.call(this, parent, "PortalPanel", DwtControl.ABSOLUTE_STYLE);

   this._welcomePanel = new WelcomePanel(this);
   this._welcomePanel.setText("Hello World");

   this._toolbar = this._makeToolbar();
   this._toolbar.zShow(true);

   this.bookmarkPanel = new BookmarkPanel(this, null, DwtControl.ABSOLUTE_STYLE);
   this.bookmarkPanel.setUI();
   this.bookmarkPanel.zShow(true);

   this._actionMenu = this._makeActionMenu()

   // XXX
   l = new AjxListener(this, this._listActionListener);
   this.bookmarkPanel.addActionListener(l);

   this.addControlListener(new AjxListener(this, this._controlListener));
}

PortalPanel.prototype = new DwtComposite();
PortalPanel.prototype.constructor = PortalPanel;

// public methods -------------------------------------------------------------

PortalPanel.prototype.refresh = function()
{
   this.bookmarkPanel.refresh();
}

PortalPanel.prototype.layout = function()
{
   var size = this.getSize();
   var width = size.x;
   var height = size.y;

   var x = 0;
   var y = 0;

   this._welcomePanel.setLocation(x, y);
   size = this._welcomePanel.getSize();
   y += size.y;

   this._toolbar.setLocation(x, y);
   size = this._toolbar.getSize();
   y += size.y;

   this.bookmarkPanel.setBounds(0, y, width, height - y);
}

PortalPanel.prototype.addSelectionListener = function(l)
{
   this.bookmarkPanel.addSelectionListener(l);
}

// private methods ------------------------------------------------------------

PortalPanel.prototype._makeToolbar = function() {
   var toolbar = new DwtToolBar(this, "ToolBar", DwtControl.ABSOLUTE_STYLE, 2);

   var b = new DwtButton(toolbar, DwtButton.ALIGN_CENTER);
   b.setText("Refresh");
   b.setToolTipContent("Display latest contents");
   b.addSelectionListener(new AjxListener(this, this._refreshButtonListener));

   b = new DwtButton(toolbar, DwtButton.ALIGN_CENTER);
   b.setText("New Bookmark");
   b.setToolTipContent("Add a new bookmark");
   this.addBookmarkButton = b;

   b = new DwtButton(toolbar, DwtButton.ALIGN_CENTER);
   b.setText("Delete");
   b.setToolTipContent("Delete selected files");
   this.deleteBookmarkButton = b;

   return toolbar;
}

PortalPanel.prototype._makeActionMenu = function()
{
   var actionMenu = new DwtMenu(this.bookmarkPanel, DwtMenu.POPUP_STYLE);

   var i = new DwtMenuItem(actionMenu, DwtMenuItem.NO_STYLE);
   i.setText("Delete");
   i.addSelectionListener(new AjxListener(this, this._deleteButtonListener));

   return actionMenu;
}

// callbacks ------------------------------------------------------------------

PortalPanel.prototype._refreshButtonListener = function()
{
   this.refresh();
}

PortalPanel.prototype._listActionListener = function(ev) {
    this._actionMenu.popup(0, ev.docX, ev.docY);
}

PortalPanel.prototype._controlListener = function()
{
   this.layout();
}
