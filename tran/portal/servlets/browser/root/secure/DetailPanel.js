// Copyright (c) 2003-2007 Untangle, Inc.
// All rights reserved.

function DetailPanel(parent, authCallback)
{
    if (0 == arguments.length) {
        return;
    }

    this._authCallback = authCallback;

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

    DwtListView.call(this, parent, "DetailPanel", DwtControl.ABSOLUTE_STYLE, header, true);
};

DetailPanel.prototype = new DwtListView();
DetailPanel.prototype.constructor = DetailPanel;

// public methods -------------------------------------------------------------

DetailPanel.prototype.chdir = function(cifsNode)
{
    if (this.cwd == cifsNode) {
        return;
    }

    this.cwd = cifsNode;

    this.refresh();
};

DetailPanel.prototype.refresh = function()
{
    var actionCb = new AjxCallback(this, this._refreshCbFn, { });

    var url = "secure/ls?url=" + this.cwd.getReqUrl() + "&type=full";
    MvRpc.invoke(null, url, null, true,
                 actionCb, MvRpc.reloadPageCallback, this._authCallback);
};

DetailPanel.prototype._refreshCbFn = function(obj, results)
{
    this._setListingXml(results.xml);
    this.setUI(1);
};

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
                                     child.getAttribute("principal"),
                                     CifsNode.TYPES[child.getAttribute("type")],
                                     child.getAttribute("size"),
                                     child.getAttribute("mtime"),
                                     child.getAttribute("content-type")));
        }
    }

    this.set(listing);
};

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
        htmlArr[idx++] = "<div style='padding: 0 4 0 4;";
        if (col.memberName == "size") {
            htmlArr[idx++] = "text-align: right;";
        }
        if (width) {
            htmlArr[idx++] = "width: " + width + ";";
        }
        htmlArr[idx++] = "'>";

        var value = item[col.memberName];
        htmlArr[idx++] = (value || "") + "</div></td>";
    }

    htmlArr[idx++] = "</tr></table>";

    div.innerHTML = htmlArr.join("");
    return div;
};

DetailPanel.prototype._sortColumn = function(col, asc)
{
    this._lastSortCol = col;
    this._lastSortAsc = asc;

    var fn = function(a, b) {
        if (col.memberName == "size") {
            av = a[col.memberName] - 0;
            bv = b[col.memberName] - 0;
        } else if (col.memberName == "lastModified") {
            av = a["lastModifiedRaw"] - 0;
            bv = b["lastModifiedRaw"] - 0;
        } else {
            av = a[col.memberName];
            bv = b[col.memberName];
        }

        return (asc ? 1 : -1) * (av < bv ? -1 : (av > bv ? 1 : 0));
    }

    this.getList().sort(fn);
    delete fn;

    this.setUI(0);
};

// DwtControl methods ---------------------------------------------------------

DetailPanel.prototype._getDnDIcon = function(dragOp)
{
    var icon = document.createElement("div");
    Dwt.setPosition(icon, Dwt.ABSOLUTE_STYLE);

    switch (dragOp) {
    case Dwt.DND_DROP_COPY:
        icon.innerHTML = "<img src='./secure/copy.gif'/>";
        break;

    case Dwt.DND_DROP_MOVE:
        icon.innerHTML = "<img src='./secure/move.gif'/>";
        break;
    }

    this.shell.getHtmlElement().appendChild(icon);
    Dwt.setZIndex(icon, Dwt.Z_DND);
    return icon;
};

DetailPanel.prototype._setDnDIconState = function(dropAllowed)
{
    this._dndIcon.className = dropAllowed
    ? this._dndIcon._origClassName + " DropAllowed"
    : this._dndIcon._origClassName + " DropNotAllowed";
};

DetailPanel.prototype._mouseOverAction = function(ev, div)
{
    var item = this.getItemFromElement(div);
    this._mouseOverItem = item;

    if (div._type == DwtListView.TYPE_LIST_ITEM) {
        if (item.tooltip) {
            this.setToolTipContent(item.tooltip);
        } else if (this._hasPreview(item.contentType)) {
            item.tooltip = "<div class='PreviewDiv'><img src='secure/scale?url=" + item.getReqUrl() + "'/></div>"
                this.setToolTipContent(item.tooltip);
        }
    }
};

DetailPanel.prototype._mouseOutAction = function(mouseEv, div)
{
    this._mouseOverItem = null;
};

// util -----------------------------------------------------------------------

DetailPanel.prototype._hasPreview = function(mimeType)
{
    var s = mimeType.split('/');
    if ("image" == s[0]) {
        var sub = s[1];
        // XXX get more complete list
        return "gif" == sub || "jpeg" == sub || "png" == sub;
    } else {
        return false;
    }
};
