// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function Browser(shell, url) {
   if (0 == arguments.length) {
      return;
   }

   var cifsNode = new CifsNode(null, url, null, CifsNode.SHARE);

   this._shell = shell;

   this._shell.addControlListener(new AjxListener(this, this._shellListener));

   DwtComposite.call(this, this._shell, "Browser", DwtComposite.ABSOLUTE_STYLE);

   this._authCallback = new AjxCallback(this, this._authResource, { });

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
   this._dirTree.addRoot(cifsNode);
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
   this._detailPanel = new DetailPanel(this, this._authCallback);
   this._detailPanel.setUI();
   this._detailPanel.zShow(true);
   this._detailPanel.addSelectionListener(new AjxListener(this, this._detailSelectionListener));
   this._detailPanel.setDragSource(dragSource);
   this._detailPanel.setDropTarget(dropTarget);

   this._actionMenu = this._makeActionMenu()
   this._detailPanel.addActionListener(new AjxListener(this, this._listActionListener));

   this._broadcastRoots();

   this.layout();

   this.chdir(cifsNode, false);

   this.zShow(true);
}

Browser.prototype = new DwtComposite();
Browser.prototype.constructor = Browser;

// fields ---------------------------------------------------------------------

Browser.CIFS_NODE = "cifsNode";

// public methods -------------------------------------------------------------

Browser.prototype.chdir = function(cifsNode, expandTree, expandDetail)
{
   this._cwd = cifsNode;

   this._addressField.setValue(cifsNode.url);

   if (undefined == expandTree || expandTree) {
      this._dirTree.chdir(cifsNode);
   }

   if (undefined == expandDetail || expandDetail) {
      this._detailPanel.chdir(cifsNode);
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
            var cifsNode = new CifsNode(null, val);
            chdir(cifsNode);
         }
      };
   }

   return toolbar;
}

Browser.prototype._makeActionMenu = function()
{
   var actionMenu = new DwtMenu(this._detailPanel, DwtMenu.POPUP_STYLE);

   var i = new DwtMenuItem(actionMenu, DwtMenuItem.NO_STYLE);
   i.setText("Delete");
   i.addSelectionListener(new AjxListener(this, this._deleteButtonListener));
   i = new DwtMenuItem(actionMenu, DwtMenuItem.NO_STYLE);
   i.setText("Rename");
   i.addSelectionListener(new AjxListener(this, this._renameButtonListener));

   return actionMenu;
}

Browser.prototype._broadcastRoots = function()
{
   var actionCb = new AjxCallback(this, this._broadcastRootsCb, new Object());

   DBG.println("INVOKE ls");
   MvRpc.invoke(null, "secure/ls?url=//", null, true,
                actionCb, MvRpc.reloadPageCallback, this._authCallback);
}

Browser.prototype._broadcastRootsCb = function(obj, results)
{
   var root = results.xml.getElementsByTagName("root")[0];
   var children = root.childNodes;
   for (var i = 0; i < children.length; i++) {
      var child = children[i];
      var tagName = child.tagName;
      if ("dir" == tagName || "file" == tagName) {
         var name = "//" + child.getAttribute("name");
         var n = new CifsNode(null, name, null, CifsNode.WORKGROUP);
         this._dirTree.addWorkGroup(n);
      }
   }
}

// listeners ------------------------------------------------------------------

Browser.prototype._detailSelectionListener = function(ev) {
   switch (ev.detail) {
      case DwtListView.ITEM_DBL_CLICKED:
      var item = ev.item;
      if (item.isDirectory()) {
         DBG.println("IS DIR");
         this.chdir(item);
      } else {
         AjxWindowOpener.open("get/" + item.getReqUrl());
      }
      break;
   }
}

// toolbar buttons ------------------------------------------------------------

Browser.prototype._dirSelectionListener = function(evt) {
   switch (evt.detail) {
      case DwtTree.ITEM_SELECTED:
      var n = evt.item.getData(Browser.CIFS_NODE);
      this.chdir(n);
      break;

      case DwtTree.ITEM_DESELECTED:
      break;

      case DwtTree.ITEM_CHECKED:
      break;

      case DwtTree.ITEM_ACTIONED:
      break;

      case DwtTree.ITEM_DBL_CLICKED:
      var item = evt.item;
      var n = item.getData(Browser.CIFS_NODE);
      if (!n.authorized) {
         var d = new LoginDialog(this._shell, n.getDomain());
         var o = { dialog: d, item: item };
         var l = new AjxListener(this, this._authenticateDialogListener, o);
         d.setButtonListener(DwtDialog.OK_BUTTON, l);
         d.popup();
      } else {
         item.setExpanded(!evt.item.getExpanded());
      }
      break;

      default:
   }
}

Browser.prototype._authenticateDialogListener = function(obj, evt)
{
   var d = obj.dialog;
   var domain = d.getDomain();
   var username = d.getUser();
   var password = d.getPassword();

   var url = "secure/login?domain=" + domain + "&username=" + username
                   + "&password=" + password;

   o = { dialog: d, item: obj.item }
   var actionCb = new AjxCallback(this, this._loginCallback, o);
   MvRpc.invoke(null, url, null, true, actionCb, MvRpc.reloadPageCallback, null);
}

Browser.prototype._loginCallback = function(obj, results)
{
   var auth = results.xml.getElementsByTagName("auth")[0];
   var status = auth.getAttribute("status");
   var principal = auth.getAttribute("principal");

   if ("success" == status) {
      obj.dialog.popdown();
      this._dirTree.repopulate(obj.item, principal);
   } else {
      alert("FAILURE");
   }
}

Browser.prototype._listActionListener = function(ev) {
    this._actionMenu.popup(0, ev.docX, ev.docY);
}


Browser.prototype._refreshButtonListener = function(ev)
{
   this.refresh();
}

Browser.prototype._uploadButtonListener = function(ev)
{
   var dialog = new FileUploadDialog(this._shell, "put", this._cwd.getReqUrl());

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

   var url = "secure/exec?command=rm";

   for (var i = 0; i < sel.length; i++) {
      url += "&file=" + sel[i].getReqUrl();
   }

   AjxRpc.invoke(null, url, null, new AjxCallback(this, this.refresh, { }),
                 false);
}

Browser.prototype._renameButtonListener = function(ev)
{
   var sel = this._detailPanel.getSelection();
   if (0 == sel.length) {
      return;
   }

   var dialog = new RenameDialog(this._shell, sel[0]);

   // XXX first selection only
   var cb = function() {
      var dest = dialog.getDest();

      if (dest) {
         var url = "secure/exec?command=rename&src=" + sel[0].getReqUrl() + "&dest=" + dest;

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

Browser.prototype._mkdirButtonListener = function(ev)
{
   var dialog = new MkdirDialog(this._shell, this._cwd.getReqUrl());

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

Browser.prototype._authResource = function(obj, response)
{
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
      break;
   }
}

// util -----------------------------------------------------------------------

Browser._mkSrcDestCommand = function(command, src, dest)
{
   var url = "secure/exec?command=" + command;

   for (var i = 0; i < src.length; i++) {
      url += "&src=" + src[i].getReqUrl(); // XXX does this get escaped ?
   }

   url += "&dest=" + dest.getReqUrl(); // XXX does this get escaped ?

   return url;
}