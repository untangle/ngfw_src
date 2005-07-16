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


/**
 *
 * @author inieves
 */
final public class RemoveProceedDialog extends MTwoButtonJDialog {
    
    public RemoveProceedDialog(String applianceName) {
        this.setTitle(applianceName + " Warning");
        this.cancelJButton.setText("<html><b>Cancel</b> remove</html>");
        this.proceedJButton.setText("<html><b>Continue</b> removing</html>");
        messageJLabel.setText("<html><center>" + applianceName + " is about to be removed from the rack.  Its settings will be lost and it will stop processing network traffic.<br><br><b>Would you like to proceed?<b></center></html>");
        this.setVisible(true);
    }
    
}
