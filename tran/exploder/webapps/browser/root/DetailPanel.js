// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function DetailPanel(parent, className, posStyle) {
   if (0 == arguments.length) {
      return;
   }

   var header = [];
   var i = 0;
   var hi = new DwtListHeaderItem("name", "Name", null, 200, false, true, true);
   hi.memberName = "name";
   header[i++] = hi;
   hi = new DwtListHeaderItem("size", "Size", null, 100, false, true, true);
   hi.memberName = "size";
   header[i++] = hi;
   hi = new DwtListHeaderItem("lastModified", "Last Modified", null, 200, false, true, true);
   hi.memberName = "lastModified";
   header[i++] = hi;

   DwtListView.call(this, parent, className, posStyle, header, true);

   this.addSelectionListener(new AjxListener(this, this._selectionListener));
}

DetailPanel.prototype = new DwtListView();
DetailPanel.prototype.constructor = DetailPanel;

// public methods -------------------------------------------------------------

DetailPanel.prototype.setListingXml = function(dom)
{
   var root = dom.getElementsByTagName("root")[0];
   this._path = root.getAttribute("path");
   var children = root.childNodes;

   var listing = new AjxVector();

   for (var i = 0; i < children.length; i++) {
      var child = children[i];
      var tagName = child.tagName;
      if ("dir" == tagName || "file" == tagName) {
         listing.add(new CifsNode(tagName,
                                  child.getAttribute("name"),
                                  child.getAttribute("size"),
                                  child.getAttribute("mtime")));
      }
   }

   this.set(listing);
}

// internal methods -----------------------------------------------------------

DetailPanel.prototype._createItemHtml = function(item) {

   var div = document.createElement("div");
   var base = "Row";
   div._styleClass = base;
   div._selectedStyleClass = [base, DwtCssStyle.SELECTED].join("-");   // Row-selected

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

DetailPanel.prototype._selectionListener = function(ev)
{
   switch (ev.detail) {
      case DwtListView.ITEM_DBL_CLICKED:
      var item = ev.item;
      if (item.type = "dir") {

      } else {
         alert("DOUBLE CLICKED: " + ev.item);
      }
      break;
   }
}