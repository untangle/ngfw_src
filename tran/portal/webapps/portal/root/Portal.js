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

   var dragSource = new DwtDragSource(Dwt.DND_DROP_MOVE | Dwt.DND_DROP_COPY);
   dragSource.addDragListener(new AjxListener(this, this._detailDragListener));
   var dropTarget = new DwtDropTarget(CifsNode);
   dropTarget.addDropListener(new AjxListener(this, this._detailDropListener));
   this._bookmarkPanel = new BookmarkPanel(this, null, DwtControl.ABSOLUTE_STYLE);
   this._bookmarkPanel.setUI();
   this._bookmarkPanel.zShow(true);
   this._bookmarkPanel.addSelectionListener(new AjxListener(this, this._detailSelectionListener));
   this._bookmarkPanel.setDragSource(dragSource);
   this._bookmarkPanel.setDropTarget(dropTarget);

   this._actionMenu = this._makeActionMenu()
   this._bookmarkPanel.addActionListener(new AjxListener(this, this._listActionListener));

   this.layout();

   this.zShow(true);

   this.login();
}

Portal.prototype = new DwtComposite();
Portal.prototype.constructor = Portal;

// fields ---------------------------------------------------------------------

Portal.CIFS_NODE = "cifsNode";

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
         var i = results.text.indexOf("success");

         if (0 <= i) {
            dialog.popdown();
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

Portal.prototype.mv = function(src, dest)
{
   var url = Portal._mkSrcDestCommand("mv", src, dest)

   // XXX handle error
   AjxRpc.invoke(null, url, null, new AjxCallback(this, this.refresh, { }), false);
}

Portal.prototype.cp = function(src, dest)
{
   var url = Portal._mkSrcDestCommand("cp", src, dest)

   // XXX handle error
   AjxRpc.invoke(null, url, null, new AjxCallback(this, this.refresh, { }), false);
}

Portal.prototype.refresh = function()
{
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
   b.setText("Upload");
   b.setToolTipContent("Upload files to share");
   b.addSelectionListener(new AjxListener(this, this._uploadButtonListener));

   b = new DwtButton(toolbar, DwtButton.ALIGN_CENTER);
   b.setText("Delete");
   b.setToolTipContent("Delete selected files");
   b.addSelectionListener(new AjxListener(this, this._deleteButtonListener));

   b = new DwtButton(toolbar, DwtButton.ALIGN_CENTER);
   b.setText("New Folder");
   b.setToolTipContent("Create a new folder");
   b.addSelectionListener(new AjxListener(this, this._mkdirButtonListener));

   return toolbar;
}

Portal.prototype._makeActionMenu = function()
{
   var actionMenu = new DwtMenu(this._bookmarkPanel, DwtMenu.POPUP_STYLE);

   var i = new DwtMenuItem(actionMenu, DwtMenuItem.NO_STYLE);
   i.setText("Delete");
   i.addSelectionListener(new AjxListener(this, this._deleteButtonListener));
   i = new DwtMenuItem(actionMenu, DwtMenuItem.NO_STYLE);
   i.setText("Rename");
   i.addSelectionListener(new AjxListener(this, this._renameButtonListener));

   return actionMenu;
}

// listeners ------------------------------------------------------------------

Portal.prototype._detailSelectionListener = function(ev) {
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

// toolbar buttons ------------------------------------------------------------

Portal.prototype._dirSelectionListener = function(ev) {
   switch (ev.detail) {
      case DwtTree.ITEM_SELECTED:
      var n = ev.item.getData(Portal.CIFS_NODE);
      break;

      case DwtTree.ITEM_DESELECTED:
      break;

      case DwtTree.ITEM_CHECKED:
      break;

      case DwtTree.ITEM_ACTIONED:
      break;

      case DwtTree.ITEM_DBL_CLICKED:
      break;

      default:
   }
}

Portal.prototype._listActionListener = function (ev) {
    this._actionMenu.popup(0, ev.docX, ev.docY);
}


Portal.prototype._refreshButtonListener = function(ev)
{
   this.refresh();
}

Portal.prototype._uploadButtonListener = function(ev)
{
   var dialog = new FileUploadDialog(this._shell, "put", this._cwd);

   dialog.addUploadCompleteListener(new AjxListener(this, this._uploadCompleteListener));

   dialog.popup();
}

Portal.prototype._uploadCompleteListener = function(evt)
{
   evt.dialog.popdown();
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

Portal.prototype._renameButtonListener = function(ev)
{
   var sel = this._bookmarkPanel.getSelection();
   if (0 == sel.length) {
      return;
   }

   var dialog = new RenameDialog(this._shell, sel[0]);

   // XXX first selection only
   var cb = function() {
      var dest = dialog.getDest();

      if (dest) {
         var url = "exec?command=rename&src=" + sel[0].url + "&dest=" + dest;

         var cb = function() {
            dialog.popdown();
            this.refresh();
         }

         AjxRpc.invoke(null, url, null, new AjxCallback(this, cb, {}), false);
      }
   }

   var l = new AjxListener(this, cb);
   dialog.setButtonListener(DwtDialog.OK_BUTTON, l);
   dialog.addListener(DwtEvent.ENTER, l);

   dialog.popup();
}

Portal.prototype._mkdirButtonListener = function(ev)
{
   var dialog = new MkdirDialog(this._shell, this._cwd);

   var cb = function() {
      var dir = dialog.getDir();

      if (dir) {
         var url = "exec?command=mkdir&url=" + dir;

         var mkdirCb = function() {
            dialog.popdown();
            this.refresh();
         }

         AjxRpc.invoke(null, url, null, new AjxCallback(this, mkdirCb, { }),
                       false);
      }
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

// dnd ------------------------------------------------------------------------

Portal.prototype._treeDragListener = function(evt)
{

}

Portal.prototype._treeDropListener = function(evt)
{
   var targetControl = evt.targetControl;

   switch (evt.action) {
      case DwtDropEvent.DRAG_ENTER:
      break;

      case DwtDropEvent.DRAG_LEAVE:
      break;

      case DwtDropEvent.DRAG_OP_CHANGED:
      window.status = "OP CHANGED!";
      break;

      case DwtDropEvent.DRAG_DROP:
      var dest = evt.targetControl.getData(Portal.CIFS_NODE);
      var src = evt.srcData;

      switch (evt.operation) {
         case Dwt.DND_DROP_COPY:
         this.cp(src, dest);
         break;

         case Dwt.DND_DROP_MOVE:
         this.mv(src, dest);
         break;
      }
      break;
   }
}

Portal.prototype._detailDragListener = function(evt)
{
   switch (evt.action) {
      case DwtDragEvent.DRAG_START:
      break;

      case DwtDragEvent.SET_DATA:
      evt.srcData = evt.srcControl.getDnDSelection();
      break;

      case DwtDragEvent.DRAG_END:
      break;
   }
}

Portal.prototype._detailDropListener = function(evt)
{
   var targetControl = evt.targetControl;

   switch (evt.action) {
      case DwtDropEvent.DRAG_ENTER:
      break;

      case DwtDropEvent.DRAG_LEAVE:
      break;

      case DwtDropEvent.DRAG_OP_CHANGED:
      window.status = "OP CHANGED!";
      break;

      case DwtDropEvent.DRAG_DROP:
      alert("DROP: " + evt.srcData);
      break;
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
