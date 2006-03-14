// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function DetailPanel(parent, className, posStyle) {
   if (0 == arguments.length) {
      return;
   }

   var i = 0;
   var header = new Array(new DwtListHeaderItem(i++, "KAKA", null, 25, false, true, true),
                          new DwtListHeaderItem(i++, "POO POO", null, 25, false, true, true));

   DwtListView.call(this, parent, className, posStyle, header, true);
}

DetailPanel.prototype = new DwtListView();
DetailPanel.prototype.constructor = DetailPanel;