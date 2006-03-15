// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function Browser(shell) {
   if (0 == arguments.length) {
      return;
   }

   try {
      DwtComposite.call(this, shell, "Browser", DwtComposite.ABSOLUTE_STYLE);

      this.dirTree = new DirTree(this, null, DwtControl.ABSOLUTE_STYLE);
      this.dirTree.addSelectionListener(new AjxListener(this, this._dirTreeListener));

      this.sash = new DwtSash(this, DwtSash.HORIZONTAL_STYLE, null, 5);
      this.detailPanel = new DetailPanel(this, null, DwtControl.ABSOLUTE_STYLE);


      var v = new AjxVector();
      v.add(new CifsNode("MEOW"));
      v.add(new CifsNode("RUFF"));

      this.detailPanel.set(v);
      this.detailPanel.setUI(0);

      this.layout();

   } catch (exn) {
      if (exn.dump) {
         alert(exn.dump());
      } else {
         alert(exn);
      }
   }
}

Browser.prototype = new DwtComposite();
Browser.prototype.constructor = Browser;

Browser.prototype.layout = function() {
   this.dirTree.setBounds(0, 0, 150, "100%");
   this.dirTree.setScrollStyle(DwtControl.SCROLL);
   this.dirTree.zShow(true);

   this.sash.setBounds(155, 0, 3, "100%");
   this.sash.zShow(true);

   this.detailPanel.setBounds(160, 0, 300, "100%");
   this.detailPanel.zShow(true);

   this.zShow(true);
}

Browser.prototype._dirTreeListener = function(ev) {
   switch (ev.detail) {
      case DwtTree.ITEM_SELECTED:
      var item = ev.item;
      var n = item.getData("smbNode");
      var url = n.url;

      var cb = function(obj, results) {
         this.detailPanel.setListingXml(results.xml);
         this.detailPanel.setUI(0);
      }

      AjxRpc.invoke(null, "ls?url=" + url + "&type=full", null,
                    new AjxCallback(this, cb, new Object()), true);
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

