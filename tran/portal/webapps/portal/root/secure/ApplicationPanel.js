// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function ApplicationPanel(parent)
{

  if (0 == arguments.length) {
    return;
  }

  DwtComposite.call(this, parent, "ApplicationPanel", DwtControl.ABSOLUTE_STYLE);

  this._toolbar = this._makeToolbar();
  this._toolbar.zShow(true);
  this._applicationList = new ApplicationList(this);
  this._applicationList.zShow(true);

  this.addControlListener(new AjxListener(this, this._controlListener));

  this._layout();
};

ApplicationPanel.prototype = new DwtComposite();
ApplicationPanel.prototype.constructor = ApplicationPanel;

// public methods -------------------------------------------------------------

ApplicationPanel.prototype.redraw = function()
{
  this._applicationList.setUI();
}

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

  var b = new DwtButton(toolbar, DwtButton.ALIGN_CENTER);
  b.setText("Refresh");
  b.setToolTipContent("Display latest contents");
  b.addSelectionListener(new AjxListener(this, this.refresh));

  return toolbar;
};

ApplicationPanel.prototype._layout = function()
{
  var size = this.getSize();

  var x = 0;
  this._toolbar.setLocation(0, 0);
  x += this._toolbar.getSize().x;
  this._applicationList.setBounds(x, 0, size.x - x, size.y);
};

ApplicationPanel.prototype._controlListener = function()
{
  this._layout();
};
