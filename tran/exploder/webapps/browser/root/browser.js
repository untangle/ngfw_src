// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function Browser(parent) {
   this.tree = new DwtTree(parent, null, null, DwtControl.ABSOLUTE_STYLE);
   this.tree.setBounds(0, 0, "150px", "100%");
   this.tree.setScrollStyle(DwtControl.SCROLL);
   this.addItems();
   this.tree.addSelectionListener(new AjxListener(this, this.treeListener));
   this.tree.reparentHtmlElement("tree-panel");
   this.tree.zShow(true);
}

Browser.run = function() {
   var shell = new DwtShell("MainShell");
   new Browser(shell);
}

Browser.prototype = {

   addItems: function() {
      var ds = this.dragSrc = new DwtDragSource(Dwt.DND_DROP_MOVE);
      ds.addDragListener(new AjxListener(this, this.dragListener));
      var dt = new DwtDropTarget(DwtTreeItem);
      dt.addDropListener(new AjxListener(this, this.dropListener));

      var n = new SmbNode("smb://bebe/");

      var root = new DwtTreeItem(this.tree);
      root.setText(n.label);
      root.setImage("Folder"); // XXX Make Server icon
      root.setData("smbNode", n);
      root.setDropTarget(dt);
      root.setToolTipContent("SMB Server");

      root.setExpanded(false);
   },

   treeListener: function(ev) {
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
   },

   dragListener: function(ev) { },

   dropListener: function(ev) { }
}