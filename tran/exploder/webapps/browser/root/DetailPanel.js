// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function DetailPanel(parent, className, posStyle) {
   if (0 == arguments.length) {
      return;
   }

   var header = [];
   var i = 0;
   var hi = new DwtListHeaderItem("name", "Name", null, 200, true, true, true);
   hi.memberName = "label";
   header[i++] = hi;
   hi = new DwtListHeaderItem("size", "Size", null, 100, true, true, true);
   hi.memberName = "size";
   header[i++] = hi;
   hi = new DwtListHeaderItem("lastModified", "Last Modified", null, 200, true, true, true);
   hi.memberName = "lastModified";
   header[i++] = hi;

   DwtListView.call(this, parent, className, posStyle, header, true);
}

DetailPanel.prototype = new DwtListView();
DetailPanel.prototype.constructor = DetailPanel;

// public methods -------------------------------------------------------------

DetailPanel.prototype.chdir = function(url)
{
   if (this.cwd == url) {
      return;
   }

   this.cwd = url;

   this.refresh();
}

DetailPanel.prototype.refresh = function()
{
   var cb = function(obj, results) {
      this._setListingXml(results.xml);
      this.setUI(1);
   }

   AjxRpc.invoke(null, "ls?url=" + this.cwd + "&type=full", null,
                 new AjxCallback(this, cb, new Object()), true);
}

// internal methods -----------------------------------------------------------

DetailPanel.prototype._setListingXml = function(dom)
{
   var root = dom.getElementsByTagName("root")[0];
   this.url = root.getAttribute("path");
   var children = root.childNodes;

   var listing = new AjxVector();

   for (var i = 0; i < children.length; i++) {
      var child = children[i];
      var tagName = child.tagName;
      if ("dir" == tagName || "file" == tagName) {
         listing.add(new CifsNode(this.url,
                                  child.getAttribute("name"),
                                  "dir" == tagName,
                                  child.getAttribute("size"),
                                  child.getAttribute("mtime"),
                                  child.getAttribute("content-type")));
      }
   }

   this.set(listing);
}

// DwtListView methods --------------------------------------------------------

DetailPanel.prototype._createItemHtml = function(item) {

   var div = document.createElement("div");
   var base = "Row";
   div._styleClass = base;
   div._selectedStyleClass = [base, DwtCssStyle.SELECTED].join("-");

   this.associateItemWithElement(item, div, DwtListView.TYPE_LIST_ITEM);
   div.className = div._styleClass;

   var htmlArr = new Array();
   var idx = 0;

    // Table
   htmlArr[idx++] = "<table cellpadding=0 cellspacing=0 border=0";
   htmlArr[idx++] = this._noMaximize ? ">" : " width=100%>";

    // Row
   htmlArr[idx++] = "<tr>";

    // Data
   for (var j = 0; j < this._headerList.length; j++) {
      var col = this._headerList[j];

      if (!col._visible) {
         continue;
      }

      htmlArr[idx++] = "<td";
      var width = AjxEnv.isIE ? (col._width + 4) : col._width;
      htmlArr[idx++] = width ? (" width=" + width + ">") : ">";
        // add a div to force clipping (TD's dont obey it)
      htmlArr[idx++] = "<div";
      htmlArr[idx++] = width ? " style='width: " + width + "'>" : ">";

      var value = item[col.memberName];
      htmlArr[idx++] = (value || "") + "</div></td>";
   }

   htmlArr[idx++] = "</tr></table>";

   div.innerHTML = htmlArr.join("");
   return div;
}

DetailPanel.prototype._sortColumn = function(col, asc)
{
   this._lastSortCol = col;
   this._lastSortAsc = asc;

   var fn = function(a, b) {
      av = a[col.memberName];
      bv = b[col.memberName];

      return (asc ? 1 : -1) * (a < b ? -1 : (a > b ? 1 : 0));
   }

   this.getList().sort(fn);
   delete fn;

   this.setUI(0);
}

// DwtControl methods ---------------------------------------------------------

DetailPanel.prototype._getDnDIcon = function(dragOp)
{
   var icon = document.createElement("div");
   Dwt.setPosition(icon, Dwt.ABSOLUTE_STYLE);

   icon.innerHTML += "<B>DRAG OPERATION:</B> "

   switch (dragOp) {
      case Dwt.DND_DROP_COPY:
      icon.innerHTML += "copy"
      break;

      case Dwt.DND_DROP_MOVE:
      icon.innerHTML += "move"
      break;
   }

   this.shell.getHtmlElement().appendChild(icon);
   Dwt.setZIndex(icon, Dwt.Z_DND);
   return icon;
}

DetailPanel.prototype._setDnDIconState = function(dropAllowed) {
   this._dndIcon.className = dropAllowed
      ? this._dndIcon._origClassName + " DropAllowed"
      : this._dndIcon._origClassName + " DropNotAllowed";
}

DetailPanel.prototype._mouseOverAction = function(ev, div)
{
   DwtListView.prototype._mouseOverAction.call(this, ev, div);

   if (div._type == DwtListView.TYPE_LIST_ITEM){
      var item = this.getItemFromElement(div);

      if ("image/jpeg" == item.contentType) {
         AjxRpc.invoke(null, "resize?url=" + item.url, null,
                       new AjxCallback(this, this._setThumbnailTooltip,
                                       { item: item }), true);
      }
   }
}

DetailPanel.prototype._setThumbnailTooltip = function(obj, results)
{
   this.setToolTipContent("<B>content-type:</B> " + obj.item.contentType);
}