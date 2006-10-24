/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle Networks, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.gui.widgets.dialogs;

import com.metavize.gui.util.Util;


final public class PowerProceedDialog extends MTwoButtonJDialog {
    
    public PowerProceedDialog(String applianceName, boolean powerOn) {
	super(Util.getMMainJFrame());
        setTitle(applianceName + " Warning");
        if(powerOn){
            cancelJButton.setIcon(Util.getButtonCancelPowerOn());
            proceedJButton.setIcon(Util.getButtonContinuePowerOn());
            messageJLabel.setText("<html><center>" + applianceName + " is about to be powered on.<br><font color=\"FF0000\">This may halt your network,<br>and disconnect you, if not configured properly.</font><br><b>Would you like to proceed?<b></center></html>");
        }
        else{
            cancelJButton.setIcon(Util.getButtonCancelPowerOff());
            proceedJButton.setIcon(Util.getButtonContinuePowerOff());
            messageJLabel.setText("<html><center>" + applianceName + " is about to be powered off.<br><font color=\"FF0000\">This may halt your network, and disconnect you.</font><br>EdgeGuard will act as a Transparent Bridge.<br><b>Would you like to proceed?<b></center></html>");
        }
        
        this.setVisible(true);
    }
    
}
