// Copyright (c) 2003-2007 Untangle, Inc.
// All rights reserved.

function ApplicationPanel(parent)
{

    if (0 == arguments.length) {
        return;
    }

    DwtComposite.call(this, parent, "ApplicationPanel", DwtControl.STATIC_STYLE);

    this._init();
};

ApplicationPanel.prototype = new DwtComposite();
ApplicationPanel.prototype.constructor = ApplicationPanel;

// constants ------------------------------------------------------------------

ApplicationPanel.DEFAULT_TITLE = "Applications";

// public methods -------------------------------------------------------------

ApplicationPanel.prototype.redraw = function()
{
    this._applicationList.setUI();
};

ApplicationPanel.prototype.setTitle = function(title)
{
    this._titleDiv.innerHTML = title || ApplicationPanel.DEFAULT_TITLE;
};

ApplicationPanel.prototype.addApplication = function(app)
{
    return this._applicationList.addApplication(app);
};

ApplicationPanel.prototype.clearApplications = function()
{
    return this._applicationList.clearApplications();
};

ApplicationPanel.prototype.addSelectionListener = function(l)
{
    this._applicationList.addSelectionListener(l);
};

ApplicationPanel.prototype.addActionListener = function(l)
{
    this._applicationList.addActionListener(l);
};

// private methods ------------------------------------------------------------

ApplicationPanel.prototype._init = function()
{
    var titleId = Dwt.getNextId();
    var listId = Dwt.getNextId();

    var html = [];
    html.push("<table width='100%' height='100%'>");

    html.push("<tr>");
    html.push("<td>");
    html.push("<div class='ListTitle' id='");
    html.push(titleId);
    html.push("'/>");
    html.push("</td>");
    html.push("</tr>");

    html.push("<tr>");

    html.push("<td style='width: 100%; height: 100%'>");
    html.push("<div style='width: 100%; height: 100%' id='");
    html.push(listId);
    html.push("'/>");
    html.push("</td>");

    html.push("</tr>");

    html.push("</table>");

    this.setContent(html.join(""));

    this._titleDiv = document.getElementById(titleId);
    this._titleDiv.innerHTML = ApplicationPanel.DEFAULT_TITLE;

    this._applicationList = new ApplicationList(this);
    if (AjxEnv.isIE) {
        this._applicationList.setScrollStyle(DwtControl.SCROLL);
    }
    this._applicationList.reparentHtmlElement(listId);
}
