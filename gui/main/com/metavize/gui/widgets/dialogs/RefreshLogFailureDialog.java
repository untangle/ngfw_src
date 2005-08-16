/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: RefreshLogFailureDialog.java 194 2005-04-06 19:13:55Z inieves $
 */

package com.metavize.gui.widgets.dialogs;


final public class RefreshLogFailureDialog extends MOneButtonJDialog {
    
    public RefreshLogFailureDialog(String applianceName) {
        this.setTitle(applianceName + " Warning");
        messageJLabel.setText("<html><center>" + applianceName + " was unable to properly refresh its event log.<br>Please try again later.</center></html>");
        this.setVisible(true);
    }
    
}
