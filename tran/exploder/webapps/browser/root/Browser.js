// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function Browser(shell) {
   if (0 == arguments.length) {
      return;
   }

   try {
      DwtComposite.call(this, shell, "Browser", DwtComposite.ABSOLUTE_STYLE);

      this.dirTree = new DirTree(this, null, DwtControl.ABSOLUTE_STYLE);
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
