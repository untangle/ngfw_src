/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: SaveProceedDialog.java 194 2005-04-06 19:13:55Z inieves $
 */

package com.metavize.gui.widgets.dialogs;

import com.metavize.gui.util.Util;


final public class SaveProceedDialog extends MTwoButtonJDialog {
    
    public SaveProceedDialog(String applianceName) {
	super(Util.getMMainJFrame(), true);
        this.setTitle(applianceName + " Warning");
        this.cancelJButton.setIcon(Util.getButtonCancelSave());
        this.proceedJButton.setIcon(Util.getButtonContinueSaving());
        messageJLabel.setText("<html><center>" + applianceName + " is about to save its settings.  These settings are critical to proper network operation and you should be sure these are the settings you want.<br><b>Your GUI may be logged out.</b><br><b>Would you like to proceed?<b></center></html>");
        this.setVisible(true);
    }
    
}
