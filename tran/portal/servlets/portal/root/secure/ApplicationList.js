// Copyright (c) 2003-2007 Untangle, Inc.
// All rights reserved.

function ApplicationList(parent)
{

    if (0 == arguments.length) {
        return;
    }

    var header = [];

    DwtListView.call(this, parent, "PortalList",
                     DwtControl.STATIC_STYLE, header);

    this.getHtmlElement().removeChild(this._listColDiv);

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
    div._selectedStyleClass = [base, DwtCssStyle.SELECTED].join("-");
    var stripeName = "Row " + "Row-" + (this._getItemIndex(item) % 2 == 0 ? "Even" : "Odd");
    div._selectedDisabledStyleClass = stripeName;
    div._styleClass = stripeName;

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
    htmlArr[idx++] = "<td";
    var width = null;
    htmlArr[idx++] = width ? (" width=" + width + ">") : ">";
    htmlArr[idx++] = "<div";
    htmlArr[idx++] = width ? " style='width: " + width + "'>" : ">";

    var value = "<img style='vertical-align: middle' src='" + item.getIconUrl() + "'/> ";
    value += "<a class='PortalListName'>" + item.description + "</a>";

    htmlArr[idx++] = (value || "") + "</div></td>";

    htmlArr[idx++] = "</tr></table>";

    div.innerHTML = htmlArr.join("");

    var evtMgr = this._evtMgr;

    var a = div.getElementsByTagName("a")[0];
    a.onclick = function() {
        if (evtMgr.isListenerRegistered(DwtEvent.SELECTION)) {
            var selEv = new DwtSelectionEvent(true);
            selEv.button = DwtMouseEvent.LEFT;
            selEv.target = div;
            selEv.item = item;
            selEv.detail = DwtListView.ITEM_DBL_CLICKED;
            evtMgr.notifyListeners(DwtEvent.SELECTION, selEv);
        }
    }

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

ApplicationList.prototype._mouseOverAction = function(ev, div)
{
    var item = this.getItemFromElement(div);
    this._mouseOverItem = item;

    if (div._type == DwtListView.TYPE_LIST_ITEM) {
        this.setToolTipContent(item.longDescription);
    }
};

ApplicationList.prototype._mouseOutAction = function(mouseEv, div)
{
    this._mouseOverItem = null;
}
