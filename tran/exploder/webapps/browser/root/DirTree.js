// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function DirTree(parent, className, posStyle) {
   if (0 == arguments.length) {
      return;
   }
   DwtTree.call(this, parent, DwtTree.SINGLE_STYLE, className, posStyle);

   this.addItems();
   this.addSelectionListener(new AjxListener(this, this._treeListener));
}

DirTree.prototype = new DwtTree();
DwtTree.prototype.constructor = DirTree;

// public methods -------------------------------------------------------------

DirTree.prototype.addItems = function() {
   var ds = this.dragSrc = new DwtDragSource(Dwt.DND_DROP_MOVE);
   ds.addDragListener(new AjxListener(this, this.dragListener));
   var dt = new DwtDropTarget(DwtTreeItem);
   dt.addDropListener(new AjxListener(this, this.dropListener));

   var n = new CifsNode(null, "smb://bebe/");

   var root = new DwtTreeItem(this);
   root.setText(n.label);
   root.setImage("Folder"); // XXX Make Server icon
   root.setData("cifsNode", n);
   root.setDropTarget(dt);
   root.setToolTipContent("SMB Server");

   root.setExpanded(false);
}

DirTree.prototype.expandNode = function(url)
{
   this._expandNode(this, url);
}

// internal methods -----------------------------------------------------------

DirTree.prototype._expandNode = function(node, url)
{
   var children = node.getItems();
   for (var i = 0; i < children.length; i++) {
      var cifsNode = children[i].getData("cifsNode");
      var substr = url.substr(0, cifsNode.url.length);
      if (substr == cifsNode.url) {
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
         }
         item.setExpanded(true);
      }

      AjxRpc.invoke(null, "ls?url=" + url + "&type=dir", null,
                    new AjxCallback(this, cb, new Object()), true);

      item.setData("expanded", true);
   } else {
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

DirTree.prototype.dragListener = function(ev) { }

DirTree.prototype.dropListener = function(ev) { }
