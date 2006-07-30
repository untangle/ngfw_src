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

package com.metavize.gui.widgets.dialogs;

import com.metavize.gui.util.Util;


final public class PowerProceedDialog extends MTwoButtonJDialog {
    
    public PowerProceedDialog(String applianceName, boolean powerOn) {
	super(Util.getMMainJFrame());
        setTitle(applianceName + " Warning");
        if(powerOn){
            cancelJButton.setIcon(Util.getButtonCancelPowerOn());
            proceedJButton.setIcon(Util.getButtonContinuePowerOn());
            messageJLabel.setText("<html><center>" + applianceName + " is about to be powered on.<br>This may halt your network if not configured properly.<br><b>Would you like to proceed?<b></center></html>");
        }
        else{
            cancelJButton.setIcon(Util.getButtonCancelPowerOff());
            proceedJButton.setIcon(Util.getButtonContinuePowerOff());
            messageJLabel.setText("<html><center>" + applianceName + " is about to be powered off.<br>This may halt your network. EdgeGuard will act as a Transparent Bridge.<br><b>Would you like to proceed?<b></center></html>");
        }
        
        this.setVisible(true);
    }
    
}
