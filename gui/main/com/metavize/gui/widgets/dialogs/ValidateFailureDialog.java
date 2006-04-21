/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: ValidateFailureDialog.java 194 2005-04-06 19:13:55Z inieves $
 */

package com.metavize.gui.widgets.dialogs;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;

final public class ValidateFailureDialog extends MOneButtonJDialog {
    
    public static ValidateFailureDialog factory(Window parentWindow, String applianceName, String componentName, String failureMessage){
	if( parentWindow instanceof Dialog )
	    return new ValidateFailureDialog((Dialog)parentWindow, applianceName, componentName, failureMessage);
	else if( parentWindow instanceof Frame )
	    return new ValidateFailureDialog((Frame)parentWindow, applianceName, componentName, failureMessage);
	else
	    return null;
    }

    private ValidateFailureDialog(Dialog parentDialog, String applianceName, String componentName, String failureMessage) {
	super(parentDialog);
	init(applianceName, componentName, failureMessage);
    }
    private ValidateFailureDialog(Frame parentFrame, String applianceName, String componentName, String failureMessage) {
	super(parentFrame);
	init(applianceName, componentName, failureMessage);
    }

    private void init(String applianceName, String componentName, String failureMessage) {
        setTitle(applianceName + " Warning");
        messageJLabel.setText("<html><center>" 
			      + applianceName 
			      + " was unable to save settings in<br>"
			      + componentName
			      + " for the following reason:<br><br><b>" 
			      + failureMessage 
			      + "</b><br>"
			      + "Please correct this and then save again.</center></html>");
        setVisible(true);
    }
    
}
