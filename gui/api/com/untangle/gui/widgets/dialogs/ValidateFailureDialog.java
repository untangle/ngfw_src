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
        if (null == applianceName) applianceName = "";
        applianceName = applianceName.trim();

        if (applianceName.length() > 0) {
            setTitle(applianceName + " Warning");
        } else {
            setTitle("Warning");
        }

        messageJLabel.setText("<html><center>"
                              + (( applianceName.length() > 0 ) ? applianceName + " was u" : "U" )
                              + "nable to save settings in<br>"
                              + componentName
                              + " for the following reason:<br><br><b>"
                              + failureMessage
                              + "</b><br>"
                              + "Please correct this and then save again.</center></html>");
        setVisible(true);
    }

}
