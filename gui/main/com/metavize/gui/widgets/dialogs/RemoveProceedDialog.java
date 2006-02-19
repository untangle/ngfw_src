/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: RemoveProceedDialog.java 194 2005-04-06 19:13:55Z inieves $
 */

package com.metavize.gui.widgets.dialogs;

import com.metavize.gui.util.Util;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;

final public class RemoveProceedDialog extends MTwoButtonJDialog {
    
    public static RemoveProceedDialog factory(Window parentWindow, String applianceName){
	if( parentWindow instanceof Dialog )
	    return new RemoveProceedDialog((Dialog)parentWindow, applianceName);
	else if( parentWindow instanceof Frame )
	    return new RemoveProceedDialog((Frame)parentWindow, applianceName);
	else
	    return null;
    }
    
    private RemoveProceedDialog(Dialog parentDialog, String applianceName) {
	super(parentDialog);
	init(applianceName);
    }
    private RemoveProceedDialog(Frame parentFrame, String applianceName) {
	super(parentFrame);
	init(applianceName);
    }

    private void init(String applianceName){
        setTitle(applianceName + " Warning");
        cancelJButton.setIcon(Util.getButtonCancelRemove());
        proceedJButton.setIcon(Util.getButtonContinueRemoving());
        messageJLabel.setText("<html><center>" + applianceName + " is about to be removed from the rack.  Its settings will be lost and it will stop processing network traffic.<br><br><b>Would you like to proceed?<b></center></html>");
        setVisible(true);
    }
    
}
