/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.gui.widgets.dialogs;

import com.untangle.gui.util.Util;


final public class PowerProceedDialog extends MTwoButtonJDialog {

    public PowerProceedDialog(String applianceName, boolean powerOn) {
        super(Util.getMMainJFrame());
        setTitle(applianceName + " Warning");
        if(powerOn){
            messageJLabel.setText("<html><center>" + applianceName + " is about to be powered on.<br><font color=\"FF0000\">This may halt your network, and disconnect you, if not configured properly.</font><br><br><b>Would you like to proceed?</b></center></html>");
        }
        else{
            messageJLabel.setText("<html><center>" + applianceName + " is about to be powered off.<br><font color=\"FF0000\">This may halt your network, and disconnect you.</font><br>Untangle Server will act as a Transparent Bridge.<br><br><b>Would you like to proceed?</b></center></html>");
        }

        this.setVisible(true);
    }

}
