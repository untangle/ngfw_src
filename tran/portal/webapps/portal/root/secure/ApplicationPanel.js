// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function ApplicationPanel(parent)
{

   if (0 == arguments.length) {
      return;
   }

   DwtComposite.call(this, parent, "ApplicationPanel", DwtControl.ABSOLUTE_STYLE);

   this._toolbar = this._makeToolbar();
   this._applicationList = new ApplicationList(this);
   this._applicationList.zShow(true);

   this.addControlListener(new AjxListener(this, this._controlListener));

   this._layout();
}

ApplicationPanel.prototype = new DwtComposite();
ApplicationPanel.prototype.constructor = ApplicationPanel;

// public methods -------------------------------------------------------------

ApplicationPanel.prototype.set = function(list)
{
   this._applicationList.set(list);
}

ApplicationPanel.prototype.getSelection = function()
{
   return this._applicationList.getSelection();
}

ApplicationPanel.prototype.addSelectionListener = function(l)
{
   this._applicationList.addSelectionListener(l);
}

ApplicationPanel.prototype.addActionListener = function(l)
{
   this._applicationList.addActionListener(l);
}

// private methods ------------------------------------------------------------

ApplicationPanel.prototype._makeToolbar = function() {
   var toolbar = new DwtToolBar(this, "VerticalToolBar", DwtControl.ABSOLUTE_STYLE, 2, 2, DwtToolBar.VERT_STYLE);

   var b = new DwtButton(toolbar, DwtButton.ALIGN_CENTER);
   b.setText("Refresh");
   b.setToolTipContent("Display latest contents");
   b.addSelectionListener(new AjxListener(this, this.refresh));

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

ApplicationPanel.prototype._layout = function()
{
   var size = this.getSize();
   DBG.println("SIZE: (" + size.x + ", " + size.y + ")");

   var x = 0;
   DBG.println("TOOLBAR AT: (0, 0)");;
   this._toolbar.setLocation(0, 0);
   x += this._toolbar.getSize().x;
   DBG.println("BOOKMARK LIST AT: (" + x + ", 0, ", + size.x - x + ", " + size.y + ")");
   this._applicationList.setBounds(x, 0, size.x - x, size.y);
}

ApplicationPanel.prototype._controlListener = function()
{
   this._layout();
}
