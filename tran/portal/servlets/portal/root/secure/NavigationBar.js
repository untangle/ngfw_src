// Copyright (c) 2003-2007 Untangle, Inc.
// All rights reserved.

function NavigationBar(parent)
{
    if (0 == arguments.length) {
        return;
    }

    DwtComposite.call(this, parent, "UntanglePanel", DwtControl.STATIC_STYLE);

    var homeButtonId = Dwt.getNextId();
    var logoutButtonId = Dwt.getNextId();
    var maximizeButtonId = Dwt.getNextId();
    var usernameId = Dwt.getNextId();
    var titleId = Dwt.getNextId();

    var html = [];
    html.push("<table style='padding: 0px 0px 0px 0px' width='100%' border='0' cellpadding='0' cellspacing='0'>");
    html.push("<tr style='height: 48px'>");
    html.push("<td width='57px'><img src='/images/Logo64x48.gif'></td>");

    html.push("<td>");
    html.push("<table width='100%' height='100%' border=0>");
    html.push("<tr><td>");
    html.push("<div style='padding: 0px 0px 0px 10px; font: bold normal normal 15pt Arial,Sans-Serif; color: #777777;' id='");
    html.push(titleId);
    html.push("'/>");
    html.push("</td></tr>");
    html.push("<tr><td>");
    html.push("<div style='padding: 0px 0px 0px 10px; font: bold normal normal 8pt Arial,Sans-Serif; color: #777777;' id='");
    html.push(usernameId);
    html.push("'/>");
    html.push("</td></tr>");
    html.push("</table>");
    html.push("</td>");

    html.push("<td style='padding: 0px 15px 0px 0px' width='50px' height='48px'>");
    html.push("<div id='");
    html.push(maximizeButtonId);
    html.push("'/>");
    html.push("</td>");

    html.push("<td width='50px' height='48px'>");
    html.push("<div id='");
    html.push(homeButtonId);
    html.push("'/>");
    html.push("</td>");

    html.push("<td width='50px' height='48px'>");
    html.push("<div id='");
    html.push(logoutButtonId);
    html.push("'/>");
    html.push("</td>");

    html.push("</table>");
    html.push("<hr style='width:100%; color:#969696; padding:0px 0px 0px 0px'/>");

    this.getHtmlElement().innerHTML = html.join("");

    this._usernameDiv = document.getElementById(usernameId);
    this._titleDiv = document.getElementById(titleId);

    this._home = new DwtButton(this,DwtLabel.ALIGN_CENTER,"DwtButton32");
    this._home.setImage("Home"); // XXX change to "Home"
    this._home.setToolTipContent("Return to Portal Homepage");
    this._home.reparentHtmlElement(homeButtonId);

    this._logout = new DwtButton(this,DwtLabel.ALIGN_CENTER,"DwtButton32");
    this._logout.setImage("Logout");
    this._logout.setToolTipContent("Logout");
    this._logout.reparentHtmlElement(logoutButtonId);

    this._maximize = new DwtButton(this,DwtLabel.ALIGN_CENTER,"DwtButton32");
    this._maximize.setImage("Maximize");
    this._maximize.setToolTipContent("Maximize content into a new window");
    this._maximize.reparentHtmlElement(maximizeButtonId);
};

NavigationBar.prototype = new DwtComposite();
NavigationBar.prototype.constructor = NavigationBar;

// public methods -------------------------------------------------------------

NavigationBar.prototype.enableMaximize = function(enabled)
{
    this._maximize.setEnabled(enabled);
};

NavigationBar.prototype.addHomeButtonListener = function(l)
{
    this._home.addSelectionListener(l);
};

NavigationBar.prototype.addLogoutButtonListener = function(l)
{
    this._logout.addSelectionListener(l);
};

NavigationBar.prototype.addMaximizeButtonListener = function(l)
{
    this._maximize.addSelectionListener(l);
};


NavigationBar.prototype.setUsername = function(username)
{
    this._usernameDiv.innerHTML = "logged in as " + username;
};

NavigationBar.prototype.setTitle = function(title)
{
    this._titleDiv.innerHTML = title;
};
