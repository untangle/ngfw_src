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
   this.setTabOrder(this._panel._fields);
}

LoginDialog.prototype.foo = function()
{
   DBG.println("HI");
}

LoginDialog.prototype = new DwtDialog();
LoginDialog.prototype.constructor = LoginDialog;

// public methods -------------------------------------------------------------

LoginPanel.prototype.println = function()
{
   return "LoginDialog";
}

LoginDialog.prototype.getUser = function()
{
   return this._panel.getUser();
}

LoginDialog.prototype.getPassword = function()
{
   return this._panel.getPassword();
}

LoginDialog.prototype.reportFailure = function(msg)
{
   this._panel.reportFailure(msg);
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