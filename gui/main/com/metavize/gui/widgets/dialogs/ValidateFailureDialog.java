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



final public class ValidateFailureDialog extends MOneButtonJDialog {
    
    public ValidateFailureDialog(String applianceName, String componentName, String failureMessage) {
        this.setTitle(applianceName + " Warning");
        messageJLabel.setText("<html><center>" 
			      + applianceName 
			      + " was unable to save settings in<br>"
			      + componentName
			      + " for the following reason:<br><br><b>" 
			      + failureMessage 
			      + "</b><br>"
			      + "<br><br>Please correct this and then save again.</center></html>");
        this.setVisible(true);
    }
    
}
