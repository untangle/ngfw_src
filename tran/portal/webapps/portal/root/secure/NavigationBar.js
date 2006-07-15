// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function NavigationBar(parent)
{
  if (0 == arguments.length) {
    return;
  }

  DwtComposite.call(this, parent, "MetavizePanel", DwtControl.ABSOLUTE_STYLE);

  var homeButtonId = Dwt.getNextId();
  var logoutButtonId = Dwt.getNextId();

  var html = [];
  html.push("<table border=0>");
  html.push("<tr>");
  html.push("<td rowspan='2'>LOGO HERE</td>");
  html.push("<td rowspan='2'>METAVIZE SECURE PORTAL POTTY</td>");
  html.push("<td>");
  html.push("<div id='");
  html.push(homeButtonId);
  html.push("'/>");
  html.push("</td>");
  html.push("</tr>");

  html.push("<tr>");
  html.push("<td>");
  html.push("<div id='");
  html.push(logoutButtonId);
  html.push("'/>");
  html.push("</td>");
  html.push("</tr>");
  html.push("</table>");

  this.getHtmlElement().innerHTML = html.join("");

  this._home = new DwtButton(this);
  this._home.setImage("Home"); // XXX change to "Home"
  this._home.setToolTipContent("Portal Home");
  this._home.reparentHtmlElement(homeButtonId);

  this._logout = new DwtButton(this);
  this._logout.setImage("Logout");
  this._logout.setToolTipContent("Logout");
  this._logout.reparentHtmlElement(logoutButtonId);
};

NavigationBar.prototype = new DwtComposite();
NavigationBar.prototype.constructor = NavigationBar;

// public methods -------------------------------------------------------------

NavigationBar.prototype.addHomeButtonListener = function(l)
{
  this._home.addSelectionListener(l);
};

NavigationBar.prototype.addLogoutButtonListener = function(l)
{
  this._logout.addSelectionListener(l);
};
