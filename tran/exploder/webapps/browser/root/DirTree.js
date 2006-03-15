// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function DirTree(parent, className, posStyle) {
   if (0 == arguments.length) {
      return;
   }
   DwtTree.call(this, parent, DwtTree.SINGLE_STYLE, className, posStyle);

   this.addItems();
   this.addSelectionListener(new AjxListener(this, this.treeListener));
}

DirTree.prototype = new DwtTree();
DwtTree.prototype.constructor = DirTree;

DirTree.prototype.addItems = function() {
   var ds = this.dragSrc = new DwtDragSource(Dwt.DND_DROP_MOVE);
   ds.addDragListener(new AjxListener(this, this.dragListener));
   var dt = new DwtDropTarget(DwtTreeItem);
   dt.addDropListener(new AjxListener(this, this.dropListener));

   var n = new SmbNode("smb://bebe/");

   var root = new DwtTreeItem(this);
   root.setText(n.label);
   root.setImage("Folder"); // XXX Make Server icon
   root.setData("smbNode", n);
   root.setDropTarget(dt);
   root.setToolTipContent("SMB Server");

   root.setExpanded(false);
}

DirTree.prototype.treeListener = function(ev) {
   var action = "";
   switch (ev.detail) {
      case DwtTree.ITEM_SELECTED:

      if (!ev.item.getData("expanded")) {
         var item = ev.item;
         var n = item.getData("smbNode");
         var url = n.url;

         var cb = function(obj, results) {
            var dom = results.xml;
            var dirs = dom.getElementsByTagName("dir");

            for (var i = 0; i < dirs.length; i++) {
               var c = dirs[i];
               var name = c.getAttribute("name");
               var n = new SmbNode(url, name);
               var tn = new DwtTreeItem(item, null, n.label, "Folder");
               tn.setData("smbNode", n);
            }

            item.setExpanded(true);
         }

         AjxRpc.invoke(null, "ls?url=" + url + "&type=dir", null,
                       new AjxCallback(this, cb, { kaka: "poo" } ), true);

         item.setData("expanded", true);
      }

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

function SmbNode(path, name)
{
   if (name) {
      this.url = path + name;
      this.label = name;
   } else {
      this.url = path;
      this.label = path;
   }

   if (this.label.length - 1 == this.label.lastIndexOf("/")) {
      this.label = this.label.substring(0, this.label.length - 1);
   }
}
