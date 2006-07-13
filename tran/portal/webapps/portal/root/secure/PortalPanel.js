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

PortalPanel.prototype.showBookmarkPanel = function(show)
{
  this.showBookmarkPanel = show;
  Dwt.setVisible(this.bookmarkPanel.getHtmlElement(), show);
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

// private constants ----------------------------------------------------------

PortalPanel._VMARGIN = 25;
PortalPanel._HMARGIN = 50;
PortalPanel._METAVIZE_PANEL_HEIGHT = 100;
PortalPanel._WELCOME_PANEL_HEIGHT = 50;

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
  var size = this.getSize();

  var left = PortalPanel._HMARGIN;
  var width = size.x - (PortalPanel._HMARGIN + PortalPanel._HMARGIN);

  var y = PortalPanel._VMARGIN;

  this.metavizePanel.setBounds(left, y, width, PortalPanel._METAVIZE_PANEL_HEIGHT);
  y += this.metavizePanel.getSize().y + PortalPanel._VMARGIN;
  var s = this.metavizePanel.getSize();

  this._welcomePanel.setBounds(left, y, width, PortalPanel._WELCOME_PANEL_HEIGHT);
  y += this._welcomePanel.getSize().y + PortalPanel._VMARGIN;

  var numPanels = 0;
  if (this.showApplicationPanel) {
    numPanels++
  }
  if (this.showBookmarkPanel) {
    numPanels++;
  }

  var vSpace = size.y - y;
  var mSize = PortalPanel._VMARGIN * numPanels;
  var pSize = Math.floor((vSpace - mSize) / numPanels);

  if (this.showApplicationPanel) {
    this.applicationPanel.setBounds(left, y, width, pSize);
    y += this.applicationPanel.getSize().y + PortalPanel._VMARGIN;
  }

  if (this.showBookmarkPanel) {
    this.bookmarkPanel.setBounds(left, y, width, pSize);
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
