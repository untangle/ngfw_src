// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function Browser(shell, url) {
   if (0 == arguments.length) {
      return;
   }

   this._shell = shell;

   this._shell.addControlListener(new AjxListener(this, this._shellListener));

   DwtComposite.call(this, this._shell, "Browser", DwtComposite.ABSOLUTE_STYLE);
   this.toolbar = this._makeToolbar();
   this.toolbar.zShow(true);

   var dragSource = new DwtDragSource(Dwt.DND_DROP_MOVE);
   dragSource.addDragListener(new AjxListener(this, this._treeDragListener));
   var dropTarget = new DwtDropTarget(CifsNode);
   dropTarget.addDropListener(new AjxListener(this, this._treeDropListener));
   this.dirTree = new DirTree(this, null, DwtControl.ABSOLUTE_STYLE,
                              dragSource, dropTarget);
   this.dirTree.setRoot(url);
   this.dirTree.setScrollStyle(DwtControl.SCROLL);
   this.dirTree.addSelectionListener(new AjxListener(this, this._dirSelectionListener));
   this.dirTree.zShow(true);

   this.sash = new DwtSash(this, DwtSash.HORIZONTAL_STYLE, null, 3);
   this.sashPos = 200;
   this.sash.registerCallback(this._sashCallback, this);
   this.sash.zShow(true);

   dragSource = new DwtDragSource(Dwt.DND_DROP_MOVE);
   dragSource.addDragListener(new AjxListener(this, this._detailDragListener));
   dropTarget = new DwtDropTarget(CifsNode);
   dropTarget.addDropListener(new AjxListener(this, this._detailDropListener));
   this.detailPanel = new DetailPanel(this, null, DwtControl.ABSOLUTE_STYLE);
   this.detailPanel.setUI(0);
   this.detailPanel.zShow(true);
   this.detailPanel.addSelectionListener(new AjxListener(this, this._detailSelectionListener));
   this.detailPanel.setDragSource(dragSource);
   this.detailPanel.setDropTarget(dropTarget);

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
   this.cwd = url;

   if (undefined == expandTree || expandTree) {
      this.dirTree.chdir(url);
   }

   if (undefined == expandDetail || expandDetail) {
      this.detailPanel.chdir(url);
   }
}

Browser.prototype.mv = function(src, dest)
{
   var url = "exec?command=mv";
   for (var i = 0; i < src.length; i++) {
      url += "&src=" + src[i].url; // XXX does this get escaped ?
   }

   url += "&dest=" + dest.url; // XXX does this get escaped ?

   // XXX handle error
   AjxRpc.invoke(null, url, null, new AjxCallback(this, this.refresh, { }), false);
}

Browser.prototype.refresh = function()
{
   this.dirTree.refresh();
   this.detailPanel.refresh();
}

Browser.prototype.layout = function(ignoreSash) {
   var size = this._shell.getSize();
   var width = size.x;
   var height = size.y;

   var x = 0;
   var y = 0;

   this.toolbar.setLocation(0, 0);
   var size = this.toolbar.getSize();
   y += size.y;

   this.dirTree.setBounds(x, y, this.sashPos, height);
   x += this.dirTree.getSize().x;

   if (!ignoreSash) {
      this.sash.setBounds(x, y, 2, height);
   }
   x += this.sash.getSize().x;

   this.detailPanel.setBounds(x, y, width - x, height);
   x += this.detailPanel.getSize().x;
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

   return toolbar;
}

// listeners ------------------------------------------------------------------

Browser.prototype._detailSelectionListener = function(ev) {
   switch (ev.detail) {
      case DwtListView.ITEM_DBL_CLICKED:
      var item = ev.item;
      if (item.type = "dir") {
         this.chdir(item.url);
      } else {
         alert("DOUBLE CLICKED: " + item);
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
   var dialog = new FileUploadDialog(this._shell, "put", this.cwd);

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
   var sel = this.detailPanel.getSelection();
   if (0 == sel.length) {
      return;
   }

   var url = "exec?command=delete";

   for (var i = 0; i < sel.length; i++) {
      url += "&file=" + sel[i].url;
   }

   AjxRpc.invoke(null, url, null, new AjxCallback(this, this.refresh, { }),
                 false);
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
   var oldPos = this.sashPos;

   this.sashPos += d;
   if (0 > this.sashPos) {
      this.sashPos = 0;
   }

   if (this._shell.getSize().x < this.sashPos) {
      this.sashPos = x;
   }

   this.layout(true);

   return this.sashPos - oldPos;
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
      break;

      case DwtDropEvent.DRAG_DROP:
      var dest = evt.targetControl.getData(Browser.CIFS_NODE);
      var src = evt.srcData;
      this.mv(src, dest);
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
      break;

      case DwtDropEvent.DRAG_DROP:
      alert("DROP: " + evt.srcData);
      break;
   }
}
