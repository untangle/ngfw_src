// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function Browser(shell, url) {
   if (0 == arguments.length) {
      return;
   }

   this._shell = shell;

   this._shell.addControlListener(new AjxListener(this, this._shellListener));

   DwtComposite.call(this, this._shell, "Browser", DwtComposite.ABSOLUTE_STYLE);
   this._toolbar = this._makeToolbar();
   this._toolbar.zShow(true);

   this._addressBar = this._makeAddressBar();
   this._addressBar.zShow(true);

   var dragSource = new DwtDragSource(Dwt.DND_DROP_MOVE | Dwt.DND_DROP_COPY);
   dragSource.addDragListener(new AjxListener(this, this._treeDragListener));
   var dropTarget = new DwtDropTarget(CifsNode);
   dropTarget.addDropListener(new AjxListener(this, this._treeDropListener));
   this._dirTree = new DirTree(this, null, DwtControl.ABSOLUTE_STYLE,
                              dragSource, dropTarget);
   this._dirTree.setRoot(url);
   this._dirTree.setScrollStyle(DwtControl.SCROLL);
   this._dirTree.addSelectionListener(new AjxListener(this, this._dirSelectionListener));
   this._dirTree.zShow(true);

   this._sash = new DwtSash(this, DwtSash.HORIZONTAL_STYLE, null, 3);
   this._sashPos = 200;
   this._sash.registerCallback(this._sashCallback, this);
   this._sash.zShow(true);

   dragSource = new DwtDragSource(Dwt.DND_DROP_MOVE | Dwt.DND_DROP_COPY);
   dragSource.addDragListener(new AjxListener(this, this._detailDragListener));
   dropTarget = new DwtDropTarget(CifsNode);
   dropTarget.addDropListener(new AjxListener(this, this._detailDropListener));
   this._detailPanel = new DetailPanel(this, null, DwtControl.ABSOLUTE_STYLE);
   this._detailPanel.setUI();
   this._detailPanel.zShow(true);
   this._detailPanel.addSelectionListener(new AjxListener(this, this._detailSelectionListener));
   this._detailPanel.setDragSource(dragSource);
   this._detailPanel.setDropTarget(dropTarget);

   this.layout();

   this.chdir(url, false);

   this.zShow(true);
}

Browser.prototype = new DwtComposite();
Browser.prototype.constructor = Browser;

// fields ---------------------------------------------------------------------

Browser.CIFS_NODE = "cifsNode";

// public methods -------------------------------------------------------------

Browser.prototype.chdir = function(url, expandTree, expandDetail)
{
   this._cwd = url;

   this._addressField.setValue(url);

   if (undefined == expandTree || expandTree) {
      this._dirTree.chdir(url);
   }

   if (undefined == expandDetail || expandDetail) {
      this._detailPanel.chdir(url);
   }
}

Browser.prototype.mv = function(src, dest)
{
   var url = Browser._mkSrcDestCommand("mv", src, dest)

   // XXX handle error
   AjxRpc.invoke(null, url, null, new AjxCallback(this, this.refresh, { }), false);
}

Browser.prototype.cp = function(src, dest)
{
   var url = Browser._mkSrcDestCommand("cp", src, dest)

   // XXX handle error
   AjxRpc.invoke(null, url, null, new AjxCallback(this, this.refresh, { }), false);
}

Browser.prototype.refresh = function()
{
   this._dirTree.refresh();
   this._detailPanel.refresh();
}

Browser.prototype.layout = function(ignoreSash) {
   var size = this._shell.getSize();
   var width = size.x;
   var height = size.y;

   var x = 0;
   var y = 0;

   this._toolbar.setLocation(0, 0);
   var size = this._toolbar.getSize();
   y += size.y;

   this._addressBar.setLocation(0, y);
   size = this._addressBar.getSize();
   y += size.y;

   this._dirTree.setBounds(x, y, this._sashPos, height);
   x += this._dirTree.getSize().x;

   if (!ignoreSash) {
      this._sash.setBounds(x, y, 2, height);
   }
   x += this._sash.getSize().x;

   this._detailPanel.setBounds(x, y, width - x, height);
   x += this._detailPanel.getSize().x;
}

// init -----------------------------------------------------------------------

Browser.prototype._makeToolbar = function() {
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

Browser.prototype._makeAddressBar = function() {
   var toolbar = new DwtToolBar(this, "ToolBar", DwtControl.ABSOLUTE_STYLE, 2);

   var l = new DwtLabel(toolbar);
   l.setText("Address");

   this._addressField = new DwtInputField({ parent: toolbar, size: 50 });

   with (this) {
      this._addressField.getInputElement().onkeyup = function(ev) {
         DwtInputField._keyUpHdlr(ev);

         var keyEv = DwtShell.keyEvent;
         keyEv.setFromDhtmlEvent(ev);
         if (DwtKeyEvent.KEY_RETURN == keyEv.keyCode) {
            var val = keyEv.dwtObj.getValue();
            if (val.charAt(val.length - 1) != '/') {
               val += '/';
            }
            chdir(val);
         }
      };
   }

   return toolbar;
}

Browser.prototype.toString = function() { return "BROWSER"; }

// listeners ------------------------------------------------------------------

Browser.prototype._detailSelectionListener = function(ev) {
   switch (ev.detail) {
      case DwtListView.ITEM_DBL_CLICKED:
      var item = ev.item;
      if (item.isDirectory) {
         DBG.println("IS DIR");
         this.chdir(item.url);
      } else {
         AjxWindowOpener.open("get/" + item.url);
      }
      break;
   }
}

// toolbar buttons ------------------------------------------------------------

Browser.prototype._dirSelectionListener = function(ev) {
   switch (ev.detail) {
      case DwtTree.ITEM_SELECTED:
      var n = ev.item.getData(Browser.CIFS_NODE);
      this.chdir(n.url);
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

Browser.prototype._refreshButtonListener = function(ev)
{
   this.refresh();
}

Browser.prototype._uploadButtonListener = function(ev)
{
   var dialog = new FileUploadDialog(this._shell, "put", this._cwd);

   dialog.addUploadCompleteListener(new AjxListener(this, this._uploadCompleteListener));

   dialog.popup();
}

Browser.prototype._uploadCompleteListener = function(evt)
{
   evt.dialog.popdown();
   this.refresh();
}

Browser.prototype._deleteButtonListener = function(ev)
{
   var sel = this._detailPanel.getSelection();
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

Browser.prototype._mkdirButtonListener = function(ev)
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

Browser.prototype._shellListener = function(ev)
{
   if (ev.oldWidth != ev.newWidth || ev.oldHeight != ev.newHeight) {
      this.layout();
   }
}

// sash -----------------------------------------------------------------------

Browser.prototype._sashCallback = function(d)
{
   var oldPos = this._sashPos;

   this._sashPos += d;
   if (0 > this._sashPos) {
      this._sashPos = 0;
   }

   if (this._shell.getSize().x < this._sashPos) {
      this._sashPos = x;
   }

   this.layout(true);

   return this._sashPos - oldPos;
}

// dnd ------------------------------------------------------------------------

Browser.prototype._treeDragListener = function(evt)
{

}

Browser.prototype._treeDropListener = function(evt)
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
      var dest = evt.targetControl.getData(Browser.CIFS_NODE);
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

Browser.prototype._detailDragListener = function(evt)
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

Browser.prototype._detailDropListener = function(evt)
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

Browser._mkSrcDestCommand = function(command, src, dest)
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
