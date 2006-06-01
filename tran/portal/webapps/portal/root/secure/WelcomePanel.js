// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function WelcomePanel(parent)
{

   DwtComposite.call(this, parent, "WelcomePanel", DwtControl.ABSOLUTE_STYLE);

   this.messageLabel = new DwtLabel(this);
}

WelcomePanel.prototype = new DwtComposite();
WelcomePanel.prototype.constructor = WelcomePanel;

// public methods -------------------------------------------------------------

WelcomePanel.prototype.setText = function(text)
{
   this.messageLabel.setText(text);
}

// private methods ------------------------------------------------------------

