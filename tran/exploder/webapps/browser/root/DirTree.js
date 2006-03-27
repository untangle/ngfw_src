// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function DirTree(parent, className, posStyle) {
   if (0 == arguments.length) {
      return;
   }
   DwtTree.call(this, parent, DwtTree.SINGLE_STYLE, className, posStyle);

   this.addSelectionListener(new AjxListener(this, this._treeSelectionListener));
   this.addTreeListener(new AjxListener(this, this._treeListener));

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

   this._populate(root);
}

DirTree.prototype.chdir = function(url)
{
   if (this.cwd == url) {
      return;
   }
   this.cwd = url;

   this._expandNode(url, this);
}

DirTree.prototype.refresh = function(url)
{
   // XXX walk tree, validating nodes
   // if this.cwd no longer valid, reset it to closest parent
}

// internal methods -----------------------------------------------------------

DirTree.prototype._expandNode = function(url, node)
{
   DBG.println("_expandNode url: " + url + " node: " + node);

   var match;

   var children = node.getItems();
   for (var i = 0; i < children.length; i++) {
      var child = children[i];
      var cifsNode = child.getData("cifsNode");

      var childUrl = cifsNode.url;
      var matches = true;

      if (childUrl.length > url.length) {
         matches = false;
      } else {
         for (var j = 0; j < childUrl.length; j++) {
            if (childUrl.charAt(j) != url.charAt(j)) {
               matches = false;
               break;
            }
         }
      }

      if (matches) {
         DBG.println("found match: " + childUrl);
         match = child;
         break;
      } else {
         DBG.println("not matches: " + childUrl);
      }
   }

   if (match) {
      if (childUrl.length == url.length) {
         DBG.println("SET EXPANDED: " + childUrl);
         this.setSelection(child, true);
      } else {
         this._populate(match, new AjxCallback(this, this._expandNode, url));
      }
   }
}

DirTree.prototype._populate = function(item, cb)
{
   var n = item.getData("cifsNode");
   DBG.println("_populate: " + n + " CB: " + cb);

   if (!item.getData("populated")) {
      item.setData("populated", true);

      var url = n.url;

      var obj = { parent: item, parentUrl: url, cb: cb };

      AjxRpc.invoke(null, "ls?url=" + url + "&type=dir", null,
                    new AjxCallback(this, this._populateCallback, obj), true);
   } else {
      cb.run(item);
   }
}

DirTree.prototype._populateCallback = function(obj, results)
{
   var dom = results.xml;
   var dirs = dom.getElementsByTagName("dir");

   for (var i = 0; i < dirs.length; i++) {
      var c = dirs[i];
      var name = c.getAttribute("name");
      var n = new CifsNode(obj.parentUrl, name);
      var tn = new DwtTreeItem(obj.parent, null, n.label, "folder");
      tn.setData("cifsNode", n);
      tn.setDragSource(this._dragSource);
      tn.setDropTarget(this._dropTarget);
   }

   if (obj.cb) {
      DBG.println("calling CB");
      obj.cb.run(obj.parent);
   } else {
      DBG.println("no CB!");
   }
}

DirTree.prototype._treeListener = function(evt)
{
   switch (evt.detail) {
      case DwtTree.ITEM_EXPANDED:
      var children = evt.item.getItems();
      for (var i = 0; i < children.length; i++) {
         this._populate(children[i]);
      }
      break;

      case DwtTree.ITEM_COLLAPSED:
      break;
   }
}

DirTree.prototype._treeSelectionListener = function(evt) {
   switch (evt.detail) {
      case DwtTree.ITEM_SELECTED:
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

DirTree.prototype._dragListener = function(evt)
{
}

DirTree.prototype._dropListener = function(evt)
{
   var targetControl = evt.targetControl;

   switch (evt.action) {
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
      DBG.println("_hoverExpand expanding: " + targetControl.getData("cifsNode"));
      targetControl.setExpanded(true);

   }
}