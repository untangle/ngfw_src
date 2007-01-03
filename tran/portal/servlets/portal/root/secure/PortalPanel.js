// Copyright (c) 2003-2007 Untangle, Inc.
// All rights reserved.

function PortalPanel(parent)
{
    if (0 == arguments.length) {
        return;
    }

    DwtComposite.call(this, parent, "PortalPanel", DwtControl.STATIC_STYLE);

    this.bookmarkPanel = new BookmarkPanel(this);
    this.applicationPanel = new ApplicationPanel(this);

    this._init();


    this._showApplicationPanel = true;
    this._showBookmarkPanel = true;

    this.refresh();
};

PortalPanel.prototype = new DwtComposite();
PortalPanel.prototype.constructor = PortalPanel;

// public methods -------------------------------------------------------------

PortalPanel.prototype.setMotd = function(motd)
{
    this._welcomePanel.getHtmlElement().innerHTML = motd
    || "Welcome to the Untangle Remote Access Portal";
};

PortalPanel.prototype.showApplicationPanel = function(show)
{
    this._showApplicationPanel = show;
    this._init();
};

PortalPanel.prototype.showBookmarkPanel = function(show)
{
    this._showBookmarkPanel = show;
    this._init();
};

PortalPanel.prototype.refresh = function()
{
    this.bookmarkPanel.refresh();
};

PortalPanel.prototype.redraw = function()
{
    this.applicationPanel.redraw();
    this.bookmarkPanel.redraw();
};

PortalPanel.prototype.addSelectionListener = function(l)
{
    this.bookmarkPanel.addSelectionListener(l);
};

// private methods ------------------------------------------------------------

PortalPanel.prototype._init = function()
{
    this._welcomePanelId = this._welcomePanelId || Dwt.getNextId();
    this._applicationPanelId = this._applicationPanelId || Dwt.getNextId();
    this._bookmarkPanelId = this._bookmarkPanelId || Dwt.getNextId();

    var colspan = 0;
    if (this._showApplicationPanel) {
        colspan++;
    }
    if (this._showBookmarkPanel) {
        colspan++;
    }

    var shell = DwtShell.getShell(window);

    if (this._welcomePanel) {
        this._welcomePanel.reparent(shell);
        this._welcomePanel.setVisible(false);
        this._welcomePanel.zShow(false);
    }

    if (this.applicationPanel) {
        this.applicationPanel.reparent(shell);
        this.applicationPanel.setVisible(false);
        this.applicationPanel.zShow(false);
    }

    if (this.bookmarkPanel) {
        this.bookmarkPanel.reparent(shell);
        this.bookmarkPanel.setVisible(false);
        this.bookmarkPanel.zShow(false);
    }

    var html = [];
    html.push("<table style='width: 100%; height: 100%;'>");

    html.push("<tr>");
    html.push("<td");
    if (0 < colspan) {
        html.push(" colspan='");
        html.push(colspan);
        html.push("'");
    }
    html.push(">");
    html.push("<div id='");
    html.push(this._welcomePanelId);
    html.push("'/>");
    html.push("</td>");
    html.push("</tr>");

    if (0 < colspan) {
        html.push("<tr style='height: 100%;'>");

        var tdStyle = " style='"
            + (2 == colspan ? "width: 50%;" : "width: 100%;")
            + "' ";

        if (this._showBookmarkPanel) {
            html.push("<td" + tdStyle + ">");
            html.push("<div style='height: 100%;' id='");
            html.push(this._bookmarkPanelId);
            html.push("'/>");
            html.push("</td>");
        }

        if (this._showApplicationPanel) {
            html.push("<td" + tdStyle + ">");
            html.push("<div style='height: 100%;' id='");
            html.push(this._applicationPanelId);
            html.push("'/>");
            html.push("</td>");
        }

        html.push("</tr>");
    }

    html.push("</table>");

    this.setContent(html.join(""));


    this._welcomePanel = this._welcomePanel || new DwtComposite(this, "WelcomePanel", DwtControl.STATIC_STYLE);
    this._welcomePanel.reparentHtmlElement(this._welcomePanelId);
    this._welcomePanel.setVisible(true);
    this._welcomePanel.zShow(true);

    if (this._showBookmarkPanel) {
        this.bookmarkPanel.reparentHtmlElement(this._bookmarkPanelId);
        this.bookmarkPanel.setVisible(true);
        this.bookmarkPanel.zShow(true);

        l = new AjxListener(this, this._listActionListener);
        this.bookmarkPanel.addActionListener(l);
    }

    if (this._showApplicationPanel) {
        this.applicationPanel.reparentHtmlElement(this._applicationPanelId);
        this.applicationPanel.setVisible(true);
        this.applicationPanel.zShow(true);
    }
};

// callbacks ------------------------------------------------------------------

PortalPanel.prototype._refreshButtonListener = function()
{
    this.refresh();
};

PortalPanel.prototype._listActionListener = function(ev)
{
    this._actionMenu.popup(0, ev.docX, ev.docY);
};
