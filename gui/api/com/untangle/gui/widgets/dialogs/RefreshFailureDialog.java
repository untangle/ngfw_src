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

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;

final public class RefreshFailureDialog extends MOneButtonJDialog {

    public static RefreshFailureDialog factory(Window parentWindow, String applianceName){
        if( parentWindow instanceof Frame )
            return new RefreshFailureDialog((Frame)parentWindow, applianceName);
        else if( parentWindow instanceof Dialog)
            return new RefreshFailureDialog((Dialog)parentWindow, applianceName);
        else
            return null;
    }

    private RefreshFailureDialog(Dialog parentDialog, String applianceName) {
        super(parentDialog);
        init(applianceName);
    }
    private RefreshFailureDialog(Frame parentFrame, String applianceName) {
        super(parentFrame);
        init(applianceName);
    }

    private void init(String applianceName){
        setTitle(applianceName + " Warning");

        if ( applianceName == null || applianceName.length() == 0 ) {
            messageJLabel.setText("<html><center>Unable to properly refresh all settings.<br>Please try again later.</center></html>");
        } else {
            messageJLabel.setText("<html><center>" + applianceName + " was unable to properly refresh all settings.<br>Please try again later.</center></html>");
        }
        setVisible(true);
    }


}
