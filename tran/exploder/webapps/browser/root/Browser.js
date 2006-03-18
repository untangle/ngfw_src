// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function Browser(shell) {
   if (0 == arguments.length) {
      return;
   }

   this.shell = shell;

/*    try { */

   this.shell.addControlListener(new AjxListener(this, this._shellListener));

   DwtComposite.call(this, this.shell, "Browser", DwtComposite.ABSOLUTE_STYLE);

   this.dirTree = new DirTree(this, null, DwtControl.ABSOLUTE_STYLE);
   this.dirTree.setScrollStyle(DwtControl.SCROLL);
   this.dirTree.addSelectionListener(new AjxListener(this, this._dirTreeListener));
   this.dirTree.zShow(true);

   this.sash = new DwtSash(this, DwtSash.HORIZONTAL_STYLE, null, 3);
   this.sashPos = 200;
   this.sash.registerCallback(this._sashCallback, this);
   this.sash.zShow(true);

   this.detailPanel = new DetailPanel(this, null, DwtControl.ABSOLUTE_STYLE);
   this.detailPanel.setUI(0);
   this.detailPanel.zShow(true);

   this.layout();

   this.zShow(true);


/*    } catch (exn) { */
/*       if (exn.dump) { */
/*          alert(exn.dump()); */
/*       } else { */
/*          alert(exn); */
/*       } */
/*    } */
}

Browser.prototype = new DwtComposite();
Browser.prototype.constructor = Browser;

Browser.prototype.layout = function(ignoreSash) {
   var size = this.shell.getSize();
   var width = size.x;
   var height = size.y;

   var x = 0;
   var y = 0;

   this.dirTree.setBounds(x, y, this.sashPos, height);
   x += this.dirTree.getSize().x;

   if (!ignoreSash) {
      this.sash.setBounds(x, y, 2, height);
   }
   x += this.sash.getSize().x;

   this.detailPanel.setBounds(x, y, width - x, height);
   x += this.detailPanel.getSize().x;
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

Browser.prototype._shellListener = function(ev)
{
   if (ev.oldWidth != ev.newWidth || ev.oldHeight != ev.newHeight) {
      this.layout();
   }
}

Browser.prototype._sashCallback = function(d)
{
   var oldPos = this.sashPos;

   this.sashPos += d;
   if (0 > this.sashPos) {
      this.sashPos = 0;
   }

   if (this.shell.getSize().x < this.sashPos) {
      this.sashPos = x;
   }

   this.layout(true);

   return this.sashPos - oldPos;
}