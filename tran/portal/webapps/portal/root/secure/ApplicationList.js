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
};

ApplicationList.prototype = new DwtListView();
ApplicationList.prototype.constructor = ApplicationList;

// public methods -------------------------------------------------------------

ApplicationList.prototype.addApplication = function(app)
{
  if (null == this._list) {
    this._list = new AjxVector();
  }
  var list = this._list;
  var len = list.size();
  for (var i = 0; i < len; i++) {
    var item = list.get(i);
    if (item.name == app.name) {
      list.replace(i, app);
      this.setUI();
      return;
    }
  }

  list.add(app);
  this.setUI();
};

ApplicationList.prototype.clearApplications = function()
{
  this._resetList();
};

// DwtListView methods --------------------------------------------------------

ApplicationList.prototype._createItemHtml = function(item)
{
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
    htmlArr[idx++] = "<div";
    htmlArr[idx++] = width ? " style='width: " + width + "'>" : ">";

    var value;
    if ("description" == col.memberName) {
      value = "<img src='" + item.getIconUrl() + "'/> " + item[col.memberName];
    } else {
      value = item[col.memberName];
    }

    htmlArr[idx++] = (value || "") + "</div></td>";
  }

  htmlArr[idx++] = "</tr></table>";

  div.innerHTML = htmlArr.join("");

  return div;
};

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
};
