// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function Portal(shell) {
    if (0 == arguments.length) {
        return;
    }

    this._shell = shell;

    DwtComposite.call(this, this._shell, "Portal");

    window.portal = this;

    this._loadApps();

    this._initLayout();

    var l = new AjxListener(this, this._homeButtonListener);
    this._navBar.addHomeButtonListener(l);
    l = new AjxListener(this, this._logoutButtonListener);
    this._navBar.addLogoutButtonListener(l);
    l = new AjxListener(this, this.maximizeApplication);
    this._navBar.addMaximizeButtonListener(l);

    l = new AjxListener(this, this._bookmarkSelectionListener);
    this._portalPanel.bookmarkPanel.addSelectionListener(l);

    l = new AjxListener(this, this._applicationSelectionListener);
    this._portalPanel.applicationPanel.addSelectionListener(l);

    l = new AjxListener(this, this._addBookmarkButtonListener);
    this._portalPanel.bookmarkPanel.addBookmarkButton.addSelectionListener(l);
    l = new AjxListener(this, this._deleteButtonListener);
    this._portalPanel.bookmarkPanel.deleteBookmarkButton.addSelectionListener(l);

    this.showPortal();

    this.refresh();
};

Portal.prototype = new DwtComposite();
Portal.prototype.constructor = Portal;

// portal api -----------------------------------------------------------------

Portal.prototype.showApplicationUrl = function(url, application, bookmark)
{
    this._currentTarget = url;

    if (this._mainPanel) {
        if (this._mainPanel != this._portalPanel) {
            this._mainPanel.dispose();
        } else {
            this._mainPanel.reparent(DwtShell.getShell(window));
            this._mainPanel.setVisible(false);
        }
    }

    this._mainPanel = new ApplicationIframe(this._shell);
    this._mainPanel.reparentHtmlElement(this._mainPanelId);
    this._mainPanel.loadUrl(url);
    this._mainPanel.zShow(true);

    this._navBar.enableMaximize(true);
};

Portal.prototype.getApplication = function(appName)
{
    var app = this._appMap[appName];
    return app;
};

Portal.prototype.splitUrl = function(url)
{
    var o = new Object();

    if (0 == url.indexOf("//")) {
        o.proto = location.protocol;
        if (":" == o.proto[o.proto.length - 1]) {
            o.proto = o.proto.substring(0, o.proto.length - 1);
        }
        var i = url.indexOf("/", 2);
        o.host = url.substring(2, i);
        o.path = url.substring(i);
    } else if (0 == url.indexOf("/")) {
        o.proto = location.protocol;
        if (":" == o.proto[o.proto.length - 1]) {
            o.proto = o.proto.substring(0, o.proto.length - 1);
        }
        o.host = location.host;
        o.path = url;
    } else {
        var i = url.indexOf(":");
        if (0 > i) {
            o.proto = location.protocol;
            if (":" == o.proto[o.proto.length - 1]) {
                o.proto = o.proto.substring(0, o.proto.length - 1);
            }
            o.host = location.hostname;
            var p = location.pathname;
            for (var k = p.length - 1; 0 <= k; k--) {
                if ("/" == p[k]) {
                    p = p.substring(0, k + 1);
                    break;
                }
            }
            o.path = p + url;
        } else {
            o.proto = url.substring(0, i);
            i = i + 3;
            var j = url.indexOf('/', i);
            if (-1 == j) {
                o.host = url.substring(i);
                o.path = '/';
            } else {
                o.host = url.substring(i, j);
                o.path = url.substring(j);
            }
        }
    }

    return o;
};

// public methods -------------------------------------------------------------

Portal.prototype.showPortal = function()
{
    this._currentTarget = null;

    if (this._mainPanel && this._mainPanel != this._portalPanel) {
        this._mainPanel.dispose();
    }
    this._mainPanel = this._portalPanel;
    this._portalPanel.reparentHtmlElement(this._mainPanelId);
    this._portalPanel.setVisible(true);

    this._navBar.enableMaximize(false);
};

Portal.prototype.maximizeApplication = function()
{
    if (this._currentTarget) {
        window.open(this._currentTarget);
        this.showPortal();
    }
}

    Portal.prototype.refresh = function()
{
    this._refreshPageInfo()
    this._loadApps();
    this._portalPanel.refresh();
};

// private methods ------------------------------------------------------------

Portal.prototype._refreshPageInfo = function()
{
    AjxRpc.invoke(null, "secure/portal?command=info", null,
                  new AjxCallback(this, this._refreshPageInfoCallback,
                                  new Object()), true);
};

// init -----------------------------------------------------------------------

Portal.prototype._initLayout = function()
{
    var navBarId = Dwt.getNextId();
    this._mainPanelId = Dwt.getNextId();

    var html = [];
    html.push("<table style='width: 100%; height: 100%;'>");

    html.push("<tr>");
    html.push("<td>");
    html.push("<div id='");
    html.push(navBarId);
    html.push("'/>");
    html.push("</td>");
    html.push("</tr>");

    html.push("<tr style='height: 100%;'>");
    html.push("<td>");
    html.push("<div style='height: 100%;' id='");
    html.push(this._mainPanelId);
    html.push("'/>");
    html.push("</td>");
    html.push("</tr>");

    html.push("</table>");

    this.setContent(html.join(""));

    this._navBar = new NavigationBar(this);
    this._navBar.reparentHtmlElement(navBarId);
    this._navBar.zShow(true);

    this._portalPanel = new PortalPanel(this._shell);
    this._portalPanel.reparentHtmlElement(this._mainPanelId);
    this._portalPanel.zShow(true);
}

    Portal.prototype._loadApps = function()
{
    AjxRpc.invoke(null, "secure/application?command=ls", null,
                  new AjxCallback(this, this._refreshAppsCallback,
                                  new Object()), true);
};

// util -----------------------------------------------------------------------

Portal._mkSrcDestCommand = function(command, src, dest)
{
    var url = "exec?command=" + command;

    for (var i = 0; i < src.length; i++) {
        url += "&src=" + src[i].url; // XXX does this get escaped ?
    }

    url += "&dest=" + dest.url; // XXX does this get escaped ?

    return url;
};

// callbacks ------------------------------------------------------------------

Portal.prototype._bookmarkSelectionListener = function(ev)
{
    switch (ev.detail) {
    case DwtListView.ITEM_SELECTED:
    this._portalPanel.applicationPanel._applicationList.deselectAll();
    break;
    case DwtListView.ITEM_DBL_CLICKED:
    var item = ev.item;
    var app = this._appMap[item.app];
    // XXX if null?
    app.openBookmark(this, item);
    break;
    }
};

Portal.prototype._applicationSelectionListener = function(ev)
{
    switch (ev.detail) {
    case DwtListView.ITEM_SELECTED:
    this._portalPanel.bookmarkPanel._bookmarkList.deselectAll();
    break;
    case DwtListView.ITEM_DBL_CLICKED:
    var item = ev.item;
    var app = this._appMap[item.name];
    // XXX if null?
    app.openApplication(this);
    break;
    }
};

Portal.prototype._refreshAppsCallback = function(obj, results)
{
    var root = results.xml.getElementsByTagName("applications")[0];

    var children = root.childNodes;

    this._appMap = { };
    this._portalPanel.applicationPanel.clearApplications();

    var appLoadCb = new AjxCallback(this, this._appLoadCallback, { });

    for (var i = 0; i < children.length; i++) {
        var child = children[i];

        if ("application" == child.tagName) {
            var name = child.getAttribute("name");
            var description = child.getAttribute("description");
            var longDescription = child.getAttribute("longDescription");
            var isHostService = "true" == child.getAttribute("isHostService");
            var appJsUrl = child.getAttribute("appJsUrl");
            var app = new Application(name, description, longDescription,
                                      isHostService, appJsUrl, appLoadCb);
        }
    }
};

Portal.prototype._appLoadCallback = function(obj, app)
{
    this._appMap[app.name] = app;

    if (!app.isHostService) {
        this._portalPanel.applicationPanel.addApplication(app);
    }

    this._portalPanel.redraw();
};

Portal.prototype._refreshPageInfoCallback = function(obj, results)
{
    var pp = this._portalPanel;

    var root = results.xml.getElementsByTagName("page-info")[0];
    document.title = root.getAttribute("title");
    var motd = root.getAttribute("motd");
    pp.setMotd(motd);

    var showApps = "true" == root.getAttribute("showApps");
    pp.showApplicationPanel(showApps);

    var showBookmarks = "true" == root.getAttribute("showBookmarks");
    pp.showBookmarkPanel(showBookmarks);

    var bmp = pp.bookmarkPanel;

    var showAddBookmarks = "true" == root.getAttribute("showAddBookmarks");
    bmp.enableAddBookmarks(showBookmarks);

    var bookmarkTitle = root.getAttribute("bookmarkTitle");
    bmp.setTitle(bookmarkTitle);
};

Portal.prototype._homeButtonListener = function()
{
    this.showPortal();
};

Portal.prototype._logoutButtonListener = function()
{
    var cb = new AjxCallback(this, this._logoutCallback);
    AjxRpc.invoke(null, "logout", null, cb, { }, true);
};

Portal.prototype._logoutCallback = function()
{
    window.location.reload();
};

Portal.prototype._addBookmarkButtonListener = function(ev)
{
    var apps = [ ];
    for (var n in this._appMap) {
        apps.push(this._appMap[n]);
    }
    var dialog = new AddBookmarkDialog(DwtShell.getShell(window), apps);

    var cb = function() {
        var bm = dialog.getBookmark();
        var url = "secure/bookmark?command=add&name=" + escape(bm.name)
        + "&app=" + escape(bm.app) + "&target=" + escape(bm.target);

        var cb = function(obj, results) {
            this.refresh();
            dialog.popdown();
        }

        AjxRpc.invoke(null, url, null, new AjxCallback(this, cb, {}), true);
    }

    var l = new AjxListener(this, cb);
    dialog.setButtonListener(DwtDialog.OK_BUTTON, l);
    dialog.addListener(DwtEvent.ENTER, l);

    dialog.popup();
};

Portal.prototype._deleteButtonListener = function(ev)
{
    var sel = this._portalPanel.bookmarkPanel.getSelection();
    if (0 == sel.length) {
        return;
    }

    var url = "secure/bookmark?command=rm";

    for (var i = 0; i < sel.length; i++) {
        url += "&id=" + sel[i].id;
    }

    AjxRpc.invoke(null, url, null, new AjxCallback(this, this.refresh, { }),
                  true);
};
