// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function MkdirDialog(parent, url)
{
   if (arguments.length == 0) {
      return;
   }

   var className = null; // XXX

   DwtDialog.call(this, parent, className, "Make Folder");

   this._panel = new MkdirPanel(this, url);
   this.addListener(DwtEvent.ONFOCUS, new AjxListener(this, this._focusListener));

   this.setView(this._panel);
}

MkdirDialog.prototype = new DwtDialog();
MkdirDialog.prototype.constructor = MkdirDialog;

// public methods -------------------------------------------------------------

MkdirDialog.prototype.getDir = function()
{
   return this._panel.getDir();
}


// internal methods -----------------------------------------------------------

MkdirDialog.prototype._focusListener = function(ev)
{
   this._panel.focus();
}

// MkdirPanel -----------------------------------------------------------------

function MkdirPanel(parent, url)
{
   if (0 == arguments.length) {
      return;
   }

   this._url = url;

   DwtComposite.call(this, parent, "MkdirPanel");

   var label = new DwtLabel(this);
   label.setText("Make new folder in " + url);
   this._field = new DwtInputField({ parent: this });
}

MkdirPanel.prototype = new DwtComposite();
MkdirPanel.prototype.constructor = FileUploadPanel;

// public methods -------------------------------------------------------------

MkdirPanel.prototype.getDir = function()
{
   // XXX resolve absolute vs relative paths
   return this._url + this._field.getValue();
}

MkdirPanel.prototype.focus = function()
{
   this._field.focus();
}