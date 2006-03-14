// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function Browser() {
   var shell = new DwtShell("MainShell");

   this.dirTree = new DirTree(shell, null, DwtControl.ABSOLUTE_STYLE);
   //this.detailPanel = new DetailPanel(shell);

   this.layout();
}

Browser.prototype = {
   layout: function() {
      this.dirTree.setBounds(0, 0, "150px", "100%");
      this.dirTree.setScrollStyle(DwtControl.SCROLL);
      this.dirTree.zShow(true);

      //this.detailPanel.setBounds("150px", 0, 0, 0);

      //this.detailPanel.zShow(true);
   }
}
