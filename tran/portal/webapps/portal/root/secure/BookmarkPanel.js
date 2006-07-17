// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function BookmarkPanel(parent)
{

  if (0 == arguments.length) {
    return;
  }

  DwtComposite.call(this, parent, "BookmarkPanel", DwtControl.ABSOLUTE_STYLE);

  this._title = new DwtLabel(this, DwtLabel.ALIGN_LEFT, "BookmarkTitle",
                             DwtControl.ABSOLUTE_STYLE);
  this._title.setText(BookmarkPanel.DEFAULT_TITLE);
  this._toolbar = this._makeToolbar();
  this._toolbar.zShow(true);
  this._bookmarkList = new BookmarkList(this);
  this._bookmarkList.zShow(true);

  this.addControlListener(new AjxListener(this, this._controlListener));

  this._layout();
};

BookmarkPanel.prototype = new DwtComposite();
BookmarkPanel.prototype.constructor = BookmarkPanel;

// constants ------------------------------------------------------------------

BookmarkPanel.DEFAULT_TITLE = "Bookmarks";

// public methods -------------------------------------------------------------

BookmarkPanel.prototype.redraw = function()
{
  this._bookmarkList.setUI();
};

BookmarkPanel.prototype.setTitle = function(title)
{
  this._title.setText(title || BookmarkPanel.DEFAULT_TITLE);
};

BookmarkPanel.prototype.enableAddBookmarks = function(enabled)
{
  this.addBookmarkButton.setEnabled(enabled);
};

BookmarkPanel.prototype.refresh = function()
{
  this._bookmarkList.refresh();
};

BookmarkPanel.prototype.getSelection = function()
{
  return this._bookmarkList.getSelection();
};

BookmarkPanel.prototype.addSelectionListener = function(l)
{
  this._bookmarkList.addSelectionListener(l);
};

BookmarkPanel.prototype.addActionListener = function(l)
{
  this._bookmarkList.addActionListener(l);
};

// private methods ------------------------------------------------------------

BookmarkPanel.prototype._makeToolbar = function() {
  var toolbar = new DwtToolBar(this, "VerticalToolBar", DwtControl.ABSOLUTE_STYLE, 2, 2, DwtToolBar.VERT_STYLE);

  var b = new DwtButton(toolbar, DwtButton.ALIGN_CENTER);
  b.setText("Refresh");
  b.setToolTipContent("Display latest contents");
  b.addSelectionListener(new AjxListener(this, this.refresh));

  var b = new DwtButton(toolbar, DwtButton.ALIGN_CENTER);
  b.setText("New Bookmark");
  b.setToolTipContent("Add a new bookmark");
  this.addBookmarkButton = b;

  b = new DwtButton(toolbar, DwtButton.ALIGN_CENTER);
  b.setText("Delete");
  b.setToolTipContent("Delete selected files");
  this.deleteBookmarkButton = b;

  return toolbar;
};

BookmarkPanel.prototype._layout = function()
{
  var size = this.getSize();

  var y = 0;
  this._title.setLocation(0, 0);
  var s = this._title.getSize();
  y += this._title.getSize().y

  var x = 0;
  this._toolbar.setBounds(0, y);
  s = this._toolbar.getSize();
  this._toolbar.setSize(s.x, size.y - y);
  x += s.x;
  this._bookmarkList.setBounds(x, y, size.x - x, size.y - y);
};

BookmarkPanel.prototype._controlListener = function()
{
  this._layout();
};
