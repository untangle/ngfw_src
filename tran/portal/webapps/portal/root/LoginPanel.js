// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function LoginPanel(parent)
{
   if (0 == arguments.length) {
      return;
   }

   DwtComposite.call(this, parent);

   this._draw();
}

LoginPanel.prototype = new DwtComposite();
LoginPanel.prototype.constructor = LoginPanel;

// public methods -------------------------------------------------------------

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
   this._draw(msg);
}

LoginPanel.prototype.focus = function()
{
   this._userField.focus();
}

// private methods ------------------------------------------------------------

LoginPanel.prototype._draw = function(msg)
{
   this.clear();

   if (msg) {
      var label = new DwtLabel(this);
      label.setText(msg);
   }

   var label = new DwtLabel(this);
   label.setText("Login:");
   this._userField = new DwtInputField({ parent: this });

   label = new DwtLabel(this);
   label.setText("Password:");
   this._passwordField = new DwtInputField({ parent: this });
}
