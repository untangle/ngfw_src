// Copyright (c) 2003-2007 Untangle, Inc.
// All rights reserved.

function BookmarkList(parent)
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

BookmarkList.prototype = new DwtListView();
BookmarkList.prototype.constructor = BookmarkList;

// public methods -------------------------------------------------------------

BookmarkList.prototype.refresh = function()
{
    var cb = new AjxCallback(this, this._bookmarkListCallback, { })

    AjxRpc.invoke(null, "secure/bookmark?command=ls", null, cb, true);
};

// internal methods -----------------------------------------------------------

BookmarkList.prototype._setListingXml = function(dom)
{
    var root = dom.getElementsByTagName("bookmarks")[0];

    var children = root.childNodes;

    var listing = new AjxVector();

    for (var i = 0; i < children.length; i++) {
        var child = children[i];

        if ("bookmark" == child.tagName) {
            listing.add(new Bookmark(child.getAttribute("id"),
                                     child.getAttribute("name"),
                                     child.getAttribute("app"),
                                     child.getAttribute("target"),
                                     child.getAttribute("type")));
        }
    }

    this.set(listing);
};

// DwtListView methods --------------------------------------------------------

BookmarkList.prototype._createItemHtml = function(item)
{
    var div = document.createElement("div");
    var base = "Row";
    div._selectedStyleClass = [base, DwtCssStyle.SELECTED].join("-");
    var stripeName = "Row " + "Row-" + (this._getItemIndex(item) % 2 == 0 ? "Even" : "Odd");
    div._selectedDisabledStyleClass = stripeName;
    div._styleClass = stripeName;

    this.associateItemWithElement(item, div, DwtListView.TYPE_LIST_ITEM);
    div.className = div._selectedDisabledStyleClass;

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

    var value;
    var app = portal.getApplication(item.app);
    if (app) {
        var iconUrl = app.getIconUrl();
        value = "<img style='vertical-align: middle' src='" + iconUrl + "'/> ";
    } else {
        // XXX put generic icon instead
    }

    value += "<a class='PortalListName'>" + item.name + "</a>";

    htmlArr[idx++] = (value || "") + "</div>"
    htmlArr[idx++] = "</td>";

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

BookmarkList.prototype._sortColumn = function(col, asc)
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

// callbacks ------------------------------------------------------------------

BookmarkList.prototype._bookmarkListCallback = function(obj, results)
{
    // XXX if error, show login dialog
    this._setListingXml(results.xml);
    this.setUI(1);
};

BookmarkList.prototype._mouseOverAction = function(ev, div)
{
    var item = this.getItemFromElement(div);
    this._mouseOverItem = item;

    if (div._type == DwtListView.TYPE_LIST_ITEM) {
        var app = portal.getApplication(item.app);
        var tooltipFn = null == app ? null : app.getTooltipFunction();
        if (tooltipFn) {
            tooltip = tooltipFn(item);
        } else {
            tooltip = "<html><b>Application:</b> " + app.description
                + "<br><b>Target: </b>" + item.target + "</html>";
        }
        this.setToolTipContent(tooltip);
    }
};

BookmarkList.prototype._mouseOutAction = function(mouseEv, div)
{
    this._mouseOverItem = null;
}
