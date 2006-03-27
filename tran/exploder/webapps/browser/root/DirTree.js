// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function DirTree(parent, className, posStyle) {
   if (0 == arguments.length) {
      return;
   }
   DwtTree.call(this, parent, DwtTree.SINGLE_STYLE, className, posStyle);

   this.addSelectionListener(new AjxListener(this, this._treeListener));

   // dragon drop
   this._dragSource = new DwtDragSource(Dwt.DND_DROP_MOVE);
   this._dragSource.addDragListener(new AjxListener(this, this._dragListener));
   this._dropTarget = new DwtDropTarget(CifsNode);
   this._dropTarget.addDropListener(new AjxListener(this, this._dropListener));
}

DirTree.prototype = new DwtTree();
DwtTree.prototype.constructor = DirTree;

// public methods -------------------------------------------------------------

DirTree.prototype.setRoot = function(url)
{
   this.cwd = url;

   var n = new CifsNode(null, url);

   var root = new DwtTreeItem(this);
   root.setText(n.label);
   root.setImage("Folder"); // XXX Make Server icon
   root.setData("cifsNode", n);

   root.setExpanded(false);
}

DirTree.prototype.chdir = function(url)
{
   if (this.cwd == url) {
      return;
   }
   this.cwd = url;

   this._expandNode(this, url);
}

DirTree.prototype.refresh = function(url)
{
   // XXX walk tree, validating nodes
   // if this.cwd no longer valid, reset it to closest parent
}

// internal methods -----------------------------------------------------------

DirTree.prototype._expandNode = function(node, url)
{
   var children = node.getItems();
   for (var i = 0; i < children.length; i++) {
      var cifsNode = children[i].getData("cifsNode");

      var childUrl = cifsNode.url;
      var matches = true;

      for (var j = 0; j < childUrl.length; j++) {
         if (childUrl.charAt(j) != url.charAt(j)) {
            matches = false;
            break;
         }
      }

      if (matches) {
         this._populateNode(children[i]);
         this._expandNode(children[i], url);
         break;
      }
   }
}

DirTree.prototype._populateNode = function(item)
{
   if (!item.getData("expanded")) {
      var n = item.getData("cifsNode");
      var url = n.url;

      var cb = function(obj, results) {
         var dom = results.xml;
         var dirs = dom.getElementsByTagName("dir");

         for (var i = 0; i < dirs.length; i++) {
            var c = dirs[i];
            var name = c.getAttribute("name");
            var n = new CifsNode(url, name);
            var tn = new DwtTreeItem(item, null, n.label, "Folder");
            tn.setData("cifsNode", n);
            tn.setDragSource(this._dragSource);
            tn.setDropTarget(this._dropTarget);
         }
         this.setSelection(item, true);
         item.setExpanded(true);
      }

      AjxRpc.invoke(null, "ls?url=" + url + "&type=dir", null,
                    new AjxCallback(this, cb, new Object()), true);

      item.setData("expanded", true);
   } else {
      this.setSelection(item, true);
      item.setExpanded(true);
   }
}

DirTree.prototype._treeListener = function(ev) {
   var action = "";
   switch (ev.detail) {
      case DwtTree.ITEM_SELECTED:

      this._populateNode(ev.item);

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

DirTree.prototype._dragListener = function(ev)
{
}

DirTree.prototype._dropListener = function(ev)
{
   var targetControl = ev.targetControl;

   switch (ev.action) {
      case DwtDropEvent.DRAG_ENTER:

      targetControl.dropSelected = true;

      DBG.println("DRAG_ENTER: " + targetControl.getData("cifsNode"));

      var act = new AjxTimedAction(this, this._hoverExpand,
                                   { targetControl: targetControl });
      AjxTimedAction.scheduleAction(act, 1000);
      break;

      case DwtDropEvent.DRAG_LEAVE:
      DBG.println("DRAG_LEAVE: " + targetControl.getData("cifsNode"));
      targetControl.dropSelected = false;
      break;

      case DwtDropEvent.DRAG_OP_CHANGED:
      break;
      case DwtDropEvent.DRAG_DROP:
      // XXX
      break;
   }
}

DirTree.prototype._hoverExpand = function(state)
{
   var targetControl = state.targetControl;
   DBG.println("_hoverExpand: " + targetControl.getData("cifsNode")
               + " DS: " + targetControl.dropSelected);

   if (targetControl.dropSelected) {
      DBG.println("_hoverExpand populating: " + targetControl.getData("cifsNode"));
      this._populateNode(targetControl);
   }
}