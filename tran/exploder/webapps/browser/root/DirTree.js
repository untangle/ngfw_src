// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function DirTree(parent, className, posStyle, dragSource, dropTarget) {
   if (0 == arguments.length) {
      return;
   }
   DwtTree.call(this, parent, DwtTree.SINGLE_STYLE, className, posStyle);

   this.addSelectionListener(new AjxListener(this, this._selectionListener));
   this.addTreeListener(new AjxListener(this, this._treeListener));

   this._dragSource = dragSource;
   this._dropTarget = dropTarget;
}

DirTree.prototype = new DwtTree();
DwtTree.prototype.constructor = DirTree;

// public methods -------------------------------------------------------------

DirTree.prototype.setRoot = function(url)
{
   this.cwd = url;

   var n = new CifsNode(null, url, true);

   var root = new DwtTreeItem(this);
   root.setText(n.label);
   root.setImage("Folder"); // XXX Make Server icon
   root.setData(Browser.CIFS_NODE, n);

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
   var match;

   var children = node.getItems();
   for (var i = 0; i < children.length; i++) {
      var child = children[i];
      var cifsNode = child.getData(Browser.CIFS_NODE);

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
         match = child;
         break;
      }
   }

   if (match) {
      if (childUrl.length == url.length) {
         this.setSelection(child, true);
      } else {
         this._populate(match, new AjxCallback(this, this._expandNode, url));
      }
   }
}

DirTree.prototype._populate = function(item, cb)
{
   var n = item.getData(Browser.CIFS_NODE);

   if (!item.getData("populated")) {
      item.setData("populated", true);

      var url = n.url;

      var obj = { parent: item, parentUrl: url, cb: cb };

      AjxRpc.invoke(null, "ls?url=" + url + "&type=dir", null,
                    new AjxCallback(this, this._populateCallback, obj), true);
   } else {
      if (cb) {
         cb.run(item);
      }
   }
}

DirTree.prototype._populateCallback = function(obj, results)
{
   var dom = results.xml;
   var dirs = dom.getElementsByTagName("dir");

   for (var i = 0; i < dirs.length; i++) {
      var c = dirs[i];
      var name = c.getAttribute("name");
      var n = new CifsNode(obj.parentUrl, name, true);
      var tn = new DwtTreeItem(obj.parent, null, n.label, "folder");
      tn.setData(Browser.CIFS_NODE, n);
      if (this._dragSource) {
         tn.setDragSource(this._dragSource);
      }
      if (this._dropTarget) {
         tn.setDropTarget(this._dropTarget);
      }
   }

   if (obj.cb) {
      obj.cb.run(obj.parent);
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

DirTree.prototype._selectionListener = function(evt)
{
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
      evt.item.setExpanded(!evt.item.getExpanded());
      break;
   }
}
