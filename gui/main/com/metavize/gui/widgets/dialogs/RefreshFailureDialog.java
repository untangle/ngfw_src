/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: RefreshFailureDialog.java 194 2005-04-06 19:13:55Z inieves $
 */

package com.metavize.gui.widgets.dialogs;



final public class RefreshFailureDialog extends MOneButtonJDialog {
    
    public RefreshFailureDialog(String applianceName) {
        this.setTitle(applianceName + " Warning");
        messageJLabel.setText("<html><center>" + applianceName + " was unable to properly refresh all settings.<br>Please try again later.</center></html>");
        this.setVisible(true);
    }
    
}
