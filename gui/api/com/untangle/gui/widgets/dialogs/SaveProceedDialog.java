/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.gui.widgets.dialogs;

import com.untangle.gui.util.Util;


final public class SaveProceedDialog extends MTwoButtonJDialog {
    
    public SaveProceedDialog(String applianceName) {
	super(Util.getMMainJFrame());
        setTitle(applianceName + " Warning");
        cancelJButton.setIcon(Util.getButtonCancelSave());
        proceedJButton.setIcon(Util.getButtonContinueSaving());
        messageJLabel.setText("<html><center>" + applianceName + " is about to save its settings.<br>"
										+ "These settings are critical to proper network operation<br>"
										+ "and you should be sure these are the settings you want.<br>"
										+ "<b>Your Untangle Client may be logged out.</b><br>"
										+ "<br><b>Would you like to proceed?<b></center></html>");
        this.setVisible(true);
    }
    
}
