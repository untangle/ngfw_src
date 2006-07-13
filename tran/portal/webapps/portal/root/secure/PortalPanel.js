// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function PortalPanel(parent)
{
  if (0 == arguments.length) {
    return;
  }

  DwtComposite.call(this, parent, "PortalPanel", DwtControl.ABSOLUTE_STYLE);

  this._init();

  // XXX
  l = new AjxListener(this, this._listActionListener);
  this.bookmarkPanel.addActionListener(l);

  this.addControlListener(new AjxListener(this, this._layout));

  this.refresh();

  this._layout();
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
  Dwt.setVisible(this.applicationPanel.getHtmlElement(), show);
  this._layout();
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
  this.metavizePanel = new DwtComposite(this, "MetavizePanel", DwtControl.ABSOLUTE_STYLE);
  var html = [];
  html.push("<img src='/images/LogoNoText96x96.gif'>");
  html.push("<bold>Metavize Secure Portal</bold>");
  this.metavizePanel.getHtmlElement().innerHTML = html.join("");

  this._welcomePanel = new DwtComposite(this, "WelcomePanel", DwtControl.ABSOLUTE_STYLE);

  this.applicationPanel = new ApplicationPanel(this);

  this.bookmarkPanel = new BookmarkPanel(this);
};

PortalPanel.prototype._layout = function()
{
  var vmargin = 25;
  var hmargin = 50;

  var size = this.getSize();

  var left = hmargin;
  var width = size.x - (hmargin + hmargin);

  var y = vmargin;

  var numPanels = this.showApplicationPanel ? 4 : 3;

  var vspace = size.y - (vmargin * (numPanels + 1));

  // XXX more appropiate sizes:

  this.metavizePanel.setBounds(left, y, width, Math.floor(vspace / numPanels));
  y += this.metavizePanel.getSize().y + vmargin;
  var s = this.metavizePanel.getSize();

  this._welcomePanel.setBounds(left, y, width, Math.floor(vspace / numPanels));
  y += this._welcomePanel.getSize().y + vmargin;

  if (this.showApplicationPanel) {
    this.applicationPanel.setBounds(left, y, width, Math.floor(vspace / numPanels));
    y += this.applicationPanel.getSize().y + vmargin;
  }

  this.bookmarkPanel.setBounds(left, y, width, Math.floor(vspace / numPanels));
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
