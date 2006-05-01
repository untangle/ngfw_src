// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function LoginDialog(parent)
{
   if (arguments.length == 0) {
      return;
   }

   var className = null; // XXX

   DwtDialog.call(this, parent, className, "Login");

   this._panel = new LoginPanel(this);
   this.addListener(DwtEvent.ONFOCUS, new AjxListener(this, this._focusListener));

   this.setView(this._panel);
}

LoginDialog.prototype = new DwtDialog();
LoginDialog.prototype.constructor = LoginDialog;

// public methods -------------------------------------------------------------

LoginDialog.prototype.getUser = function()
{
   return this._panel.getUser();
}

LoginDialog.prototype.getPassword = function()
{
   return this._panel.getPassword();
}


// internal methods -----------------------------------------------------------

LoginDialog.prototype._uploadCompleteListener = function(evt)
{
   evt.dialog = this;

   this.notifyListeners(FileUploadPanel.UPLOAD_COMPLETE, evt);
}

LoginDialog.prototype._focusListener = function(ev)
{
   this._panel.focus();
}

