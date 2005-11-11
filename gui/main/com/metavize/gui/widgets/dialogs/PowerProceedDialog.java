/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: PowerProceedDialog.java 194 2005-04-06 19:13:55Z inieves $
 */

package com.metavize.gui.widgets.dialogs;

import com.metavize.gui.util.Util;


final public class PowerProceedDialog extends MTwoButtonJDialog {
    
    public PowerProceedDialog(String applianceName, boolean powerOn) {
	super(Util.getMMainJFrame(), true);
        this.setTitle(applianceName + " Warning");
        if(powerOn){
            this.cancelJButton.setIcon(Util.getButtonCancelPowerOn());
            this.proceedJButton.setIcon(Util.getButtonContinuePowerOn());
            messageJLabel.setText("<html><center>" + applianceName + " is about to be powered on.<br><br><b>Would you like to proceed?<b></center></html>");
        }
        else{
            this.cancelJButton.setIcon(Util.getButtonCancelPowerOff());
            this.proceedJButton.setIcon(Util.getButtonContinuePowerOff());
            messageJLabel.setText("<html><center>" + applianceName + " is about to be powered off.<br><br><b>Would you like to proceed?<b></center></html>");
        }
        
        this.setVisible(true);
    }
    
}
