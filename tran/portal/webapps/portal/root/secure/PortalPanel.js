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

   this._bookmarkPanel = new BookmarkPanel(this, null, DwtControl.ABSOLUTE_STYLE);
   this._bookmarkPanel.setUI();
   this._bookmarkPanel.zShow(true);

   this._actionMenu = this._makeActionMenu()

   // XXX
   l = new AjxListener(this, this._listActionListener);
   this._bookmarkPanel.addActionListener(l);

   this.addControlListener(new AjxListener(this, this._controlListener));
}

PortalPanel.prototype = new DwtComposite();
PortalPanel.prototype.constructor = PortalPanel;

// public methods -------------------------------------------------------------

PortalPanel.prototype.refresh = function()
{
   this._bookmarkPanel.refresh();
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

   this._bookmarkPanel.setBounds(0, y, width, height - y);
}

PortalPanel.prototype.addSelectionListener = function(l)
{
   this._bookmarkPanel.addSelectionListener(l);
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
   b.addSelectionListener(new AjxListener(this, this._addBookmarkButtonListener));

   b = new DwtButton(toolbar, DwtButton.ALIGN_CENTER);
   b.setText("Delete");
   b.setToolTipContent("Delete selected files");
   b.addSelectionListener(new AjxListener(this, this._deleteButtonListener));

   return toolbar;
}

PortalPanel.prototype._makeActionMenu = function()
{
   var actionMenu = new DwtMenu(this._bookmarkPanel, DwtMenu.POPUP_STYLE);

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

PortalPanel.prototype._deleteButtonListener = function(ev)
{
   var sel = this._bookmarkPanel.getSelection();
   if (0 == sel.length) {
      return;
   }

   var url = "exec?command=rm";

   for (var i = 0; i < sel.length; i++) {
      url += "&file=" + sel[i].url;
   }

   AjxRpc.invoke(null, url, null, new AjxCallback(this, this.refresh, { }),
                 false);
}

PortalPanel.prototype._addBookmarkButtonListener = function(ev)
{
   var dialog = new AddBookmarkDialog(DwtShell.getShell(window), this._apps);

   var cb = function() {
      var bm = dialog.getBookmark();
      var url = "bookmark?command=add&name=" + bm.name
         + "&app=" + bm.app + "&target=" + bm.target;

      var cb = function(obj, results) {
         this.refresh();
         dialog.popdown();
      }

      AjxRpc.invoke(null, url, null, new AjxCallback(this, cb, {}), true);
   }

   var l = new AjxListener(this, cb);
   dialog.setButtonListener(DwtDialog.OK_BUTTON, l);
   dialog.addListener(DwtEvent.ENTER, l);

   dialog.popup();
}

PortalPanel.prototype._controlListener = function()
{
   this.layout();
}
