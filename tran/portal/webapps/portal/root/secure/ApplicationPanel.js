// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function ApplicationPanel(parent)
{

  if (0 == arguments.length) {
    return;
  }

  DwtComposite.call(this, parent, "ApplicationPanel", DwtControl.ABSOLUTE_STYLE);

  this._title = new DwtLabel(this, DwtLabel.ALIGN_LEFT, "ListTitle",
                             DwtControl.ABSOLUTE_STYLE);
  this._title.setText(ApplicationPanel.DEFAULT_TITLE);
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

ApplicationPanel.prototype._layout = function()
{
  var size = this.getSize();

  var y = 0;
  this._title.setLocation(0, 0);
  var s = this._title.getSize();
  y += this._title.getSize().y

  var x = 0;

  this._applicationList.setBounds(x, y, size.x - x, size.y - y);
};

ApplicationPanel.prototype._controlListener = function()
{
  this._layout();
};
