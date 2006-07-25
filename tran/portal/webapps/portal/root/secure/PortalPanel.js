// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function PortalPanel(parent)
{
    if (0 == arguments.length) {
        return;
    }

    DwtComposite.call(this, parent, "PortalPanel", DwtControl.RELATIVE_STYLE);

    this._init();

    // XXX
    l = new AjxListener(this, this._listActionListener);
    this.bookmarkPanel.addActionListener(l);

    this.refresh();
};

PortalPanel.prototype = new DwtComposite();
PortalPanel.prototype.constructor = PortalPanel;

// public methods -------------------------------------------------------------

PortalPanel.prototype.setMotd = function(motd)
{
    this._welcomePanel.getHtmlElement().innerHTML = motd
    || "Welcome to Metavize Secure Portal";
};

PortalPanel.prototype.showApplicationPanel = function(show)
{
    this.showApplicationPanel = show;
    this._init();
};

PortalPanel.prototype.showBookmarkPanel = function(show)
{
    this.showBookmarkPanel = show;
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

// private constants ----------------------------------------------------------

PortalPanel._VMARGIN = 5;
PortalPanel._HMARGIN = 100;
PortalPanel._WELCOME_PANEL_HEIGHT = 50;

// private methods ------------------------------------------------------------

PortalPanel.prototype._init = function()
{
    this._welcomePanelId = this._welcomePanelId || Dwt.getNextId();
    this._applicationPanelId = this._applicationPanelId || Dwt.getNextId();
    this._bookmarkPanelId = this._bookmarkPanelId || Dwt.getNextId();

    var colspan = 0;
    if (this.showApplicationPanel) {
        colspan++;
    }
    if (this.showBookmarkPanel) {
        colspan++;
    }

    var shell = DwtShell.getShell(window);

    if (this._welcomePanel) {
        this._welcomePanel.reparent(shell);
        this._welcomePanel.setVisible(false);
    }

    if (this.applicationPanel) {
        this.applicationPanel.reparent(shell);
        this.applicationPanel.setVisible(false);
    }

    if (this.bookmarkPanel) {
        this.bookmarkPanel.reparent(shell);
        this.bookmarkPanel.setVisible(false);
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

        if (this.showBookmarkPanel) {
            html.push("<td>");
            html.push("<div style='height: 100%;' id='");
            html.push(this._bookmarkPanelId);
            html.push("'/>");
            html.push("</td>");
        }

        if (this.showApplicationPanel) {
            html.push("<td>");
            html.push("<div style='height: 100%;' id='");
            html.push(this._applicationPanelId);
            html.push("'/>");
            html.push("</td>");
        }

        html.push("</tr>");
    }

    html.push("</table>");

    this.setContent(html.join(""));

    this._welcomePanel = this._welcomePanel || new DwtComposite(this, "WelcomePanel", DwtControl.RELATIVE_STYLE);
    this._welcomePanel.reparentHtmlElement(this._welcomePanelId);
    this._welcomePanel.setVisible(true);

    if (this.showBookmarkPanel) {
        this.bookmarkPanel = this.bookmarkPanel || new BookmarkPanel(this);
        this.bookmarkPanel.reparentHtmlElement(this._bookmarkPanelId);
        this.bookmarkPanel.setVisible(true);
    }

    if (this.showApplicationPanel) {
        this.applicationPanel = this.applicationPanel || new ApplicationPanel(this);
        this.applicationPanel.reparentHtmlElement(this._applicationPanelId);
        this.applicationPanel.setVisible(true);
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
