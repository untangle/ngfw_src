// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function Portal(shell, url) {
   if (0 == arguments.length) {
      return;
   }

   this._shell = shell;

   this._shell.addControlListener(new AjxListener(this, this._shellListener));

   DwtComposite.call(this, this._shell, "Portal", DwtComposite.ABSOLUTE_STYLE);

   this._loadApps();

   this._toolbar = this._makeToolbar();
   this._toolbar.zShow(true);

   this._desktop = new Desktop(this, null, DwtControl.ABSOLUTE_STYLE);
   this._desktop.zShow(true);

   this.layout();

   this.zShow(true);
}

Portal.prototype = new DwtComposite();
Portal.prototype.constructor = Portal;

// portal api -----------------------------------------------------------------

Portal.prototype.openPage = function(url)
{
   window.open(url);
}

Portal.prototype.splitUrl = function(url)
{
   var o = new Object();

   if (0 == url.indexOf("//")) {
      o.proto = location.protocol;
      if (":" == o.proto[o.proto.length - 1]) {
         o.proto = o.proto.substring(0, o.proto.length - 1);
      }
      var i = url.indexOf("/", 2);
      o.host = url.substring(2, i);
      o.path = url.substring(i);
   } else if (0 == url.indexOf("/")) {
      o.proto = location.protocol;
      if (":" == o.proto[o.proto.length - 1]) {
         o.proto = o.proto.substring(0, o.proto.length - 1);
      }
      o.host = location.host;
      o.path = url;
   } else {
      var i = url.indexOf(":");
      if (0 > i) {
         o.proto = location.protocol;
         if (":" == o.proto[o.proto.length - 1]) {
            o.proto = o.proto.substring(0, o.proto.length - 1);
         }
         o.host = location.hostname;
         var p = location.pathname;
         for (var k = p.length - 1; 0 <= k; k--) {
            if ("/" == p[k]) {
               p = p.substring(0, k + 1);
               break;
            }
         }
         o.path = p + url;
      } else {
         o.proto = url.substring(0, i);
         i = i + 3;
         var j = url.indexOf('/', i);
         o.host = url.substring(i, j);
         o.path = url.substring(j);
      }
   }

   return o;
}

// public methods -------------------------------------------------------------

Portal.prototype.layout = function() {
   var size = this._shell.getSize();
   var width = size.x;
   var height = size.y;

   this._toolbar.setLocation(0, 0);
   var y = this._toolbar.getSize().y;
   this._desktop.setLocation(0, y);
   this._desktop.setSize(width, height - y);
}

// private fields -------------------------------------------------------------

Portal._BOOKMARK_KEY;

// init -----------------------------------------------------------------------

Portal.prototype._loadApps = function()
{
   AjxRpc.invoke(null, "secure/application?command=ls", null,
                 new AjxCallback(this, this._refreshAppsCallback,
                                 new Object()), true);
}

Portal.prototype._makeToolbar = function() {
   var toolbar = new DwtToolBar(this, "ToolBar", DwtControl.ABSOLUTE_STYLE, 2);

   var b = new DwtButton(toolbar, DwtButton.ALIGN_CENTER);
   b.setText("Portal");

   var menu = new DwtMenu(b, DwtMenu.DROPDOWN_STYLE);
   b.setMenu(menu);

   this._addBookmarks(menu);

   var b = new DwtButton(toolbar, DwtButton.ALIGN_CENTER);
   b.setText("Bookmark Manager");
   b.addSelectionListener(new AjxListener(this, this._bookmarkManagerButtonListener));

   return toolbar;
}

Portal.prototype._addBookmarks = function(menu)
{
   AjxRpc.invoke(null, "secure/bookmark?command=ls", null,
                 new AjxCallback(this, this._addBookmarksCallback, { menu: menu }), true);
}

// listeners ------------------------------------------------------------------

Portal.prototype._bookmarkItemListener = function(ev) {
   var item = ev.item;
   var bm = item.getData(Portal._BOOKMARK_KEY);
   DBG.println("GOT BM: " + bm);
   DBG.println("GOT BM APP: " + bm.app);
   var app = this._appMap[bm.app];
   DBG.println("GOT APP: " + app);
      // XXX if null?
   app.openBookmark(this, item.target);
}

Portal.prototype._bookmarkManagerButtonListener = function(ev) {
   var dialog = new BookmarkManagerDialog(this._shell);

   var l = new AjxListener(this, this._bookmarkManagerDialogListener);
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

// callbacks ------------------------------------------------------------------

Portal.prototype._bookmarkManagerDialogListener = function(evt)
{
   var dialog = evt.item.parent;
   dialog.popdown();
}

Portal.prototype._refreshAppsCallback = function(obj, results) {
   var root = results.xml.getElementsByTagName("applications")[0];

   var children = root.childNodes;

   this._apps = new Array();
   this._appMap = new Object();

   for (var i = 0; i < children.length; i++) {
      var child = children[i];

      if ("application" == child.tagName) {
         var name = child.getAttribute("name");
         var appJs = child.getElementsByTagName("appJs")[0].firstChild.data;
         var app = new Application(name, appJs);
         this._apps.push(app);
         this._appMap[name] = app;
         DBG.println("this._appMap[" + name + "] = " + app);
      }
   }
}

Portal.prototype._addBookmarksCallback = function(obj, results)
{
   var menu = obj.menu;

   var l = this._getBookmarks(results.xml);
   // XXX sort these bookmarks by type
   for (var i = 0; i < l.length; i++) {
      var bm = l[i];
      var mi = new DwtMenuItem(menu);
      mi.setData(Portal._BOOKMARK_KEY, bm);
      DBG.println("SET DATA TO: " + mi.getData(Portal._BOOKMARK_KEY));
      mi.setText(bm.name);
      mi.addSelectionListener(new AjxListener(this, this._bookmarkItemListener));
   }
}

Portal.prototype._getBookmarks = function(dom)
{
   var root = dom.getElementsByTagName("bookmarks")[0];

   var children = root.childNodes;

   var l = [];

   for (var i = 0; i < children.length; i++) {
      var child = children[i];

      if ("bookmark" == child.tagName) {
         l.push(new Bookmark(child.getAttribute("id"),
                             child.getAttribute("name"),
                             child.getAttribute("app"),
                             child.getAttribute("target")));
      }
   }

   return l;
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
