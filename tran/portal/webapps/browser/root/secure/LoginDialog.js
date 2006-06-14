// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function LoginDialog(parent, domain)
{
   if (arguments.length == 0) {
      return;
   }

   var className = null; // XXX

   DwtDialog.call(this, parent, className, "Login");

   this._panel = new LoginPanel(this, domain);
   this.addListener(DwtEvent.ONFOCUS, new AjxListener(this, this._focusListener));

   this.setView(this._panel);
   this.setTabOrder(this._panel._fields);
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

LoginDialog.prototype.getDomain = function()
{
   return this._panel.getDomain();
}

LoginDialog.prototype.reportFailure = function(msg)
{
   this._panel.reportFailure(msg);
}

// internal methods -----------------------------------------------------------

LoginDialog.prototype._focusListener = function(ev)
{
   this._panel.focus();
}

// ----------------------------------------------------------------------------
// Login Panel
// ----------------------------------------------------------------------------

function LoginPanel(parent, domain)
{
   if (0 == arguments.length) {
      return;
   }

   this.domain = domain;

   DwtComposite.call(this, parent);

   this._init();
}

LoginPanel.prototype = new DwtComposite();
LoginPanel.prototype.constructor = LoginPanel;

// public methods -------------------------------------------------------------

LoginPanel.prototype.println = function()
{
   return "LoginPanel";
}

LoginPanel.prototype.getDomain = function()
{
   return this.domain;
}

LoginPanel.prototype.getUser = function()
{
   return this._userField.getValue();
}

LoginPanel.prototype.getPassword = function()
{
   return this._passwordField.getValue();
}

LoginPanel.prototype.reportFailure = function(msg)
{
   this._showError(msg);
}

LoginPanel.prototype.focus = function()
{
   this._userField.focus();
}

// private methods ------------------------------------------------------------

LoginPanel.prototype._init = function()
{
   this._fields = new Array();

   this._domainLabel = new DwtLabel(this);
   this._domainLabel.setText("Authenticate for domain: " + this.domain);
   this._fields.push(this._domainLabel);

   this._msgLabel = new DwtLabel(this);
   this._msgLabel.setVisible(false);
   this._fields.push(this._msgLabel);

   var label = new DwtLabel(this);
   label.setText("Login:");
   this._userField = new DwtInputField({ parent: this });
   this._fields.push(this._userField);

   label = new DwtLabel(this);
   label.setText("Password:");
   this._passwordField = new DwtInputField({ parent: this,
      type: DwtInputField.PASSWORD });
   this._fields.push(this._passwordField);
}

LoginPanel.prototype._showError = function(msg)
{
   this._msgLabel.setText(msg);
   this._msgLabel.setVisible(true);
}

