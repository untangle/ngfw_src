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

   this.addControlListener(new AjxListener(this, this._controlListener));

   this.refresh()

   this._layout();
}

PortalPanel.prototype = new DwtComposite();
PortalPanel.prototype.constructor = PortalPanel;

// public methods -------------------------------------------------------------

PortalPanel.prototype.refresh = function()
{
   this.bookmarkPanel.refresh();
}

PortalPanel.prototype.addSelectionListener = function(l)
{
   this.bookmarkPanel.addSelectionListener(l);
}

PortalPanel.prototype.getSelection = function()
{
   return this.bookmarkPanel.getSelection();
}

// private methods ------------------------------------------------------------

PortalPanel.prototype._init = function()
{
   this.welcomePanel = new DwtComposite(this, "WelcomePanel", DwtControl.ABSOLUTE_STYLE);
   this.welcomePanel.getHtmlElement().innerHTML = "<blink>hello world</blink>"

   this.bookmarkPanel = new BookmarkPanel(this);
}

PortalPanel.prototype._layout = function()
{
   var size = this.getSize();

   var margin = 50;

   var left = margin;
   var width = size.x - (margin + margin);

   var y = margin;

   this.welcomePanel.setBounds(left, y, width, 100);

   y += this.welcomePanel.getSize().y + 25;

   this.bookmarkPanel.setBounds(left, y, width, size.y - (margin + y));
}

// callbacks ------------------------------------------------------------------

PortalPanel.prototype._refreshButtonListener = function()
{
   this.refresh();
}

PortalPanel.prototype._listActionListener = function(ev) {
    this._actionMenu.popup(0, ev.docX, ev.docY);
}

PortalPanel.prototype._controlListener = function()
{
   this._layout();
}

