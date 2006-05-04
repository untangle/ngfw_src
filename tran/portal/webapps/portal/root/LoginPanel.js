// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function LoginPanel(parent)
{
   if (0 == arguments.length) {
      return;
   }

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
