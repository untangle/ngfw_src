// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function BookmarkPanel(parent)
{

    if (0 == arguments.length) {
        return;
    }

    DwtComposite.call(this, parent, "BookmarkPanel", DwtControl.STATIC_STYLE);

    this._init();
};

BookmarkPanel.prototype = new DwtComposite();
BookmarkPanel.prototype.constructor = BookmarkPanel;

// constants ------------------------------------------------------------------

BookmarkPanel.DEFAULT_TITLE = "Bookmarks";

// public methods -------------------------------------------------------------

BookmarkPanel.prototype.redraw = function()
{
    this._bookmarkList.setUI();
};

BookmarkPanel.prototype.setTitle = function(title)
{
    this._titleDiv.innerHTML = title || BookmarkPanel.DEFAULT_TITLE;
};

BookmarkPanel.prototype.enableAddBookmark = function(enabled)
{
    this.addBookmarkButton.setEnabled(enabled);
    this.editBookmarkButton.setEnabled(enabled);
    this.deleteBookmarkButton.setEnabled(enabled);
};

BookmarkPanel.prototype.refresh = function()
{
    this._bookmarkList.refresh();
};

BookmarkPanel.prototype.getSelection = function()
{
    return this._bookmarkList.getSelection();
};

BookmarkPanel.prototype.addSelectionListener = function(l)
{
    this._bookmarkList.addSelectionListener(l);
};

BookmarkPanel.prototype.addActionListener = function(l)
{
    this._bookmarkList.addActionListener(l);
};

// private methods ------------------------------------------------------------

BookmarkPanel.prototype._makeToolbar = function() {
    var toolbar = new DwtToolBar(this, "PortalToolBar", DwtControl.STATIC_STYLE, 2, 2, DwtToolBar.VERT_STYLE);

    var b = new DwtButton(toolbar,DwtLabel.ALIGN_CENTER,"DwtButton32");
    b.setImage("Add");
    b.setDisabledImage("AddDisabled");
    b.setToolTipContent("Add a new bookmark");
    this.addBookmarkButton = b;

    b = new DwtButton(toolbar,DwtLabel.ALIGN_CENTER,"DwtButton32");
    b.setText("Edit"); // XXX change to setImage
    //b.setDisabledImage("AddDisabled");
    b.setToolTipContent("Edit bookmark");
    this.editBookmarkButton = b;

    b = new DwtButton(toolbar,DwtButton.ALIGN_CENTER,"DwtButton32");
    b.setImage("Remove");
    b.setDisabledImage("RemoveDisabled");
    b.setToolTipContent("Remove selected bookmarks");
    this.deleteBookmarkButton = b;

    return toolbar;
};

BookmarkPanel.prototype._init = function()
{
    var titleId = Dwt.getNextId();
    var toolbarId = Dwt.getNextId();
    var listId = Dwt.getNextId();

    var html = [];
    html.push("<table width='100%' height='100%'>");

    html.push("<tr>");
    html.push("<td colspan='2'>");
    html.push("<div class='ListTitle' id='");
    html.push(titleId);
    html.push("'/>");
    html.push("</td>");
    html.push("</tr>");

    html.push("<tr>");

    html.push("<td>");
    html.push("<div id='");
    html.push(toolbarId);
    html.push("'/>");
    html.push("</td>");

    html.push("<td style='width: 100%; height: 100%'>");
    html.push("<div style='width: 100%; height: 100%' id='");
    html.push(listId);
    html.push("'/>");
    html.push("</td>");

    html.push("</tr>");

    html.push("</table>");

    this.setContent(html.join(""));

    this._titleDiv = document.getElementById(titleId);
    this._titleDiv.innerHTML = BookmarkPanel.DEFAULT_TITLE;

    this._toolbar = this._makeToolbar();
    this._toolbar.reparentHtmlElement(toolbarId);

    this._bookmarkList = new BookmarkList(this);
    this._bookmarkList.setScrollStyle(DwtControl.SCROLL);
    this._bookmarkList.reparentHtmlElement(listId);
}
