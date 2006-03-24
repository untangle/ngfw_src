// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function Browser(shell, url) {
   if (0 == arguments.length) {
      return;
   }

   this._shell = shell;

/*    try { */

   this._shell.addControlListener(new AjxListener(this, this._shellListener));

   DwtComposite.call(this, this._shell, "Browser", DwtComposite.ABSOLUTE_STYLE);

   this.toolbar = new DwtToolBar(this, "ToolBar", DwtControl.ABSOLUTE_STYLE, 2);
   var b = new DwtButton(this.toolbar, DwtButton.ALIGN_CENTER);
   b.setText("Upload");
   b.addSelectionListener(new AjxListener(this, this._uploadButtonListener));

   this.toolbar.zShow(true);

   this.dirTree = new DirTree(this, null, DwtControl.ABSOLUTE_STYLE);
   this.dirTree.setRoot(url);
   this.dirTree.setScrollStyle(DwtControl.SCROLL);
   this.dirTree.addSelectionListener(new AjxListener(this, this._dirTreeSelectionListener));
   this.dirTree.zShow(true);

   this.sash = new DwtSash(this, DwtSash.HORIZONTAL_STYLE, null, 3);
   this.sashPos = 200;
   this.sash.registerCallback(this._sashCallback, this);
   this.sash.zShow(true);

   this.detailPanel = new DetailPanel(this, null, DwtControl.ABSOLUTE_STYLE);
   this.detailPanel.setUI(0);
   this.detailPanel.zShow(true);
   this.detailPanel.addSelectionListener(new AjxListener(this, this._detailSelectionListener));

   this.layout();

   this.chdir(url, false);

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

// public methods -------------------------------------------------------------

Browser.prototype.chdir = function(url, expandTree, expandDetail)
{
   this.cwd = url;

   if (undefined == expandTree || expandTree) {
      this.dirTree.chdir(url);
   }

   if (undefined == expandDetail || expandDetail) {
      this.detailPanel.chdir(url);
   }
}

Browser.prototype.layout = function(ignoreSash) {
   var size = this._shell.getSize();
   var width = size.x;
   var height = size.y;

   var x = 0;
   var y = 0;

   this.toolbar.setLocation(0, 0);
   var size = this.toolbar.getSize();
   y += size.y;

   this.dirTree.setBounds(x, y, this.sashPos, height);
   x += this.dirTree.getSize().x;

   if (!ignoreSash) {
      this.sash.setBounds(x, y, 2, height);
   }
   x += this.sash.getSize().x;

   this.detailPanel.setBounds(x, y, width - x, height);
   x += this.detailPanel.getSize().x;
}

// listeners ------------------------------------------------------------------

Browser.prototype._detailSelectionListener = function(ev) {
   switch (ev.detail) {
      case DwtListView.ITEM_DBL_CLICKED:
      var item = ev.item;
      if (item.type = "dir") {
         this.chdir(item.url);
      } else {
         alert("DOUBLE CLICKED: " + item);
      }
      break;
   }
}

Browser.prototype._dirTreeSelectionListener = function(ev) {
   switch (ev.detail) {
      case DwtTree.ITEM_SELECTED:
      var n = ev.item.getData("cifsNode");
      this.chdir(n.url);
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

Browser.prototype._uploadButtonListener = function(ev)
{
   var dialog = new FileUploadDialog(this._shell, "put", this.cwd);

   var cb = function(evt) {
      dialog.popdown();
      this.detailPanel.refresh();
   }

   dialog.addUploadCompleteListener(new AjxListener(this, cb));

   dialog.popup();
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

   if (this._shell.getSize().x < this.sashPos) {
      this.sashPos = x;
   }

   this.layout(true);

   return this.sashPos - oldPos;
}