/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.mail.gui;

import com.metavize.gui.util.Util;
import com.metavize.gui.widgets.dialogs.*;
import java.awt.Dialog;

final public class QuarantinePurgeProceedDialog extends MTwoButtonJDialog {
    
    public QuarantinePurgeProceedDialog(Dialog topLevelDialog) {
	super( topLevelDialog, true );
        this.setTitle("Email Quarantine Purge Warning");
        this.cancelJButton.setIcon(null);
        this.proceedJButton.setIcon(null);
	this.cancelJButton.setText("<html><b>Cancel</b> purge</html>");
	this.proceedJButton.setText("<html><b>Continue</b> purging</html>");
        messageJLabel.setText("<html><center>Purging emails will permanently delete them<br>"
			      + "so that no one can view them.<br>"
			      + "Purged emails cannot be recovered."
			      + "<br><b>Would you like to purge?<b></center></html>");
        this.setVisible(true);
    }
    
}
