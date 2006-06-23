// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function ApplicationList(parent)
{

   if (0 == arguments.length) {
      return;
   }

   var header = [];
   var hi = new DwtListHeaderItem("description", "Application", null, 200, true, true, true);
   hi.memberName = "description";
   header.push(hi);

   DwtListView.call(this, parent, "ApplicationList", DwtControl.ABSOLUTE_STYLE, header);

   this.setUI(0);
}

ApplicationList.prototype = new DwtListView();
ApplicationList.prototype.constructor = ApplicationList;

// DwtListView methods --------------------------------------------------------

ApplicationList.prototype._createItemHtml = function(item) {
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

ApplicationList.prototype._sortColumn = function(col, asc)
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
