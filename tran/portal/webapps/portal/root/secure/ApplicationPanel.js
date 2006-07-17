// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function ApplicationPanel(parent)
{

  if (0 == arguments.length) {
    return;
  }

  DwtComposite.call(this, parent, "ApplicationPanel", DwtControl.ABSOLUTE_STYLE);

  this._title = new DwtLabel(this, DwtLabel.ALIGN_LEFT, "ApplicationTitle",
                             DwtControl.ABSOLUTE_STYLE);
  this._title.setText(ApplicationPanel.DEFAULT_TITLE);
  this._toolbar = this._makeToolbar();
  this._toolbar.zShow(true);
  this._applicationList = new ApplicationList(this);
  this._applicationList.zShow(true);

  this.addControlListener(new AjxListener(this, this._controlListener));

  this._layout();
};

ApplicationPanel.prototype = new DwtComposite();
ApplicationPanel.prototype.constructor = ApplicationPanel;

// constants ------------------------------------------------------------------

ApplicationPanel.DEFAULT_TITLE = "Applications";

// public methods -------------------------------------------------------------

ApplicationPanel.prototype.redraw = function()
{
  this._applicationList.setUI();
}

ApplicationPanel.prototype.setTitle = function(title)
{
  this._title.setText(title || ApplicationPanel.DEFAULT_TITLE);
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

ApplicationPanel.prototype._makeToolbar = function() {
  var toolbar = new DwtToolBar(this, "VerticalToolBar", DwtControl.ABSOLUTE_STYLE, 2, 2, DwtToolBar.VERT_STYLE);

  return toolbar;
};

ApplicationPanel.prototype._layout = function()
{
  var size = this.getSize();

  var y = 0;
  this._title.setLocation(0, 0);
  var s = this._title.getSize();
  y += this._title.getSize().y

  var x = 0;
  this._toolbar.setLocation(0, y);
  s = this._toolbar.getSize();
  this._toolbar.setSize(s.x, size.y - y);
  x += s.x;
  this._applicationList.setBounds(x, y, size.x - x, size.y - y);
};

ApplicationPanel.prototype._controlListener = function()
{
  this._layout();
};
