// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function Portal(shell, url) {
   if (0 == arguments.length) {
      return;
   }

   this._shell = shell;

   this._shell.addControlListener(new AjxListener(this, this._shellListener));

   DwtComposite.call(this, this._shell, "Portal", DwtComposite.ABSOLUTE_STYLE);
   this._toolbar = this._makeToolbar();
   this._toolbar.zShow(true);

   this._bookmarkPanel = new BookmarkPanel(this, null, DwtControl.ABSOLUTE_STYLE);
   this._bookmarkPanel.setUI();
   this._bookmarkPanel.zShow(true);
   this._bookmarkPanel.addSelectionListener(new AjxListener(this, this._bookmarkSelectionListener));

   this._actionMenu = this._makeActionMenu()
   this._bookmarkPanel.addActionListener(new AjxListener(this, this._listActionListener));

   this.layout();

   this.zShow(true);

   this.login();
}

Portal.prototype = new DwtComposite();
Portal.prototype.constructor = Portal;

// public methods -------------------------------------------------------------

Portal.prototype.login = function()
{
   var dialog = new LoginDialog(this._shell);

   var cb = function() {
      var user = dialog.getUser();
      var password = dialog.getPassword();

      // XXX XXX put in post
      var url = "login?user=" + user + "&password=" + password;

      var cb = function(obj, results) {
         if (results.success) {
            dialog.popdown();
            this.refresh();
         } else {
            dialog.reportFailure("bad login");
         }
      }

      AjxRpc.invoke(null, url, null, new AjxCallback(this, cb, {}), false);
   }

   var l = new AjxListener(this, cb);
   dialog.setButtonListener(DwtDialog.OK_BUTTON, l);
   dialog.addListener(DwtEvent.ENTER, l);

   dialog.popup();
}

Portal.prototype.refresh = function()
{
   var cb = function(obj, results) {
      var root = results.xml.getElementsByTagName("applications")[0];

      var children = root.childNodes;

      this._apps = new Array();

      for (var i = 0; i < children.length; i++) {
         var child = children[i];

         if ("application" == child.tagName) {
            this._apps.push(new Application(child.getAttribute("name")))
         }
      }
   }

   AjxRpc.invoke(null, "application?command=ls", null,
                 new AjxCallback(this, cb, new Object()), true);

   this._bookmarkPanel.refresh();
}

Portal.prototype.layout = function() {
   var size = this._shell.getSize();
   var width = size.x;
   var height = size.y;

   var x = 0;
   var y = 0;

   this._toolbar.setLocation(0, 0);
   var size = this._toolbar.getSize();
   y += size.y;

   this._bookmarkPanel.setBounds(x, y, width - x, height);
   x += this._bookmarkPanel.getSize().x;
}

// init -----------------------------------------------------------------------

Portal.prototype._makeToolbar = function() {
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

Portal.prototype._makeActionMenu = function()
{
   var actionMenu = new DwtMenu(this._bookmarkPanel, DwtMenu.POPUP_STYLE);

   var i = new DwtMenuItem(actionMenu, DwtMenuItem.NO_STYLE);
   i.setText("Delete");
   i.addSelectionListener(new AjxListener(this, this._deleteButtonListener));

   return actionMenu;
}

// listeners ------------------------------------------------------------------

Portal.prototype._bookmarkSelectionListener = function(ev) {
   switch (ev.detail) {
      case DwtListView.ITEM_DBL_CLICKED:
      var item = ev.item;
      if (item.isDirectory) {
         DBG.println("IS DIR");
      } else {
         AjxWindowOpener.open("get/" + item.url);
      }
      break;
   }
}

Portal.prototype._listActionListener = function(ev) {
    this._actionMenu.popup(0, ev.docX, ev.docY);
}


Portal.prototype._refreshButtonListener = function(ev)
{
   this.refresh();
}

Portal.prototype._deleteButtonListener = function(ev)
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

Portal.prototype._addBookmarkButtonListener = function(ev)
{
   var dialog = new AddBookmarkDialog(this._shell, this._apps);

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

// shell ----------------------------------------------------------------------

Portal.prototype._shellListener = function(ev)
{
   if (ev.oldWidth != ev.newWidth || ev.oldHeight != ev.newHeight) {
      this.layout();
   }
}

// util -----------------------------------------------------------------------

Portal._mkSrcDestCommand = function(command, src, dest)
{
   var url = "exec?command=" + command;

   for (var i = 0; i < src.length; i++) {
      url += "&src=" + src[i].url; // XXX does this get escaped ?
   }

   url += "&dest=" + dest.url; // XXX does this get escaped ?

   return url;
}

DwtControl._mouseOverHdlr =
function(ev) {
    // Check to see if a drag is occurring. If so, don't process the mouse
    // over events.
    var captureObj = (DwtMouseEventCapture.getId() == "DwtControl") ? DwtMouseEventCapture.getCaptureObj() : null;
    if (captureObj != null) {
        ev = DwtUiEvent.getEvent(ev);
        ev._stopPropagation = true;
        return false;
    }
    var obj = DwtUiEvent.getDwtObjFromEvent(ev);
    if (!obj) return false;

    var mouseEv = DwtShell.mouseEvent;
    if (obj._dragging == DwtControl._NO_DRAG) {
        mouseEv.setFromDhtmlEvent(ev);
        if (obj.isListenerRegistered(DwtEvent.ONMOUSEOVER))
            obj.notifyListeners(DwtEvent.ONMOUSEOVER, mouseEv);
        // Call the tooltip after the listeners to give them a
        // chance to change the tooltip text.
        if (obj._toolTipContent != null) {
            var shell = DwtShell.getShell(window);
            var manager = shell.getHoverMgr();
            if ((manager.getHoverObject() != this || !manager.isHovering()) && !DwtMenu.menuShowing()) {
                manager.reset();
                manager.setHoverObject(this);
                manager.setHoverOverData(obj);
                manager.setHoverOverDelay(DwtToolTip.TOOLTIP_DELAY);
                manager.setHoverOverListener(obj._hoverOverListener);
                manager.hoverOver(mouseEv.docX, mouseEv.docY);
            }
        }
    }
    mouseEv._stopPropagation = true;
    mouseEv._returnValue = false;
    mouseEv.setToDhtmlEvent(ev);
    return false;
};
