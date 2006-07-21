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
  html.push("<table style='padding: 50px 50px 0px 50px' width='100%' height='96px' border=0>");
  html.push("<tr>");
  html.push("<td width='57px'><img src='/images/LogoNoText48x48.gif'></td>");
  html.push("<td style='font: bold normal normal 25pt Arial,Sans-Serif; color: #777777;'>Metavize Secure Portal</td>");
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
  html.push("</tr>");
  html.push("<tr>");
  html.push("<td colspan='4' style='padding: 0px 0px 0px 0px'>");
  html.push("<hr width='100%' size='1' color='#969696'/>");
  html.push("</td>");
  html.push("</tr>");
  html.push("</table>");

  this.getHtmlElement().innerHTML = html.join("");

  this._home = new DwtButton(this,DwtLabel.ALIGN_CENTER,"DwtButton32");
  this._home.setImage("Home"); // XXX change to "Home"
  this._home.setToolTipContent("Return to Portal Homepage");
  this._home.reparentHtmlElement(homeButtonId);

  this._logout = new DwtButton(this,DwtLabel.ALIGN_CENTER,"DwtButton32");
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
