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


final public class SaveFailureDialog extends MOneButtonJDialog {

    public static SaveFailureDialog factory(Window parentWindow, String applianceName){
        if( parentWindow instanceof Frame )
            return new SaveFailureDialog((Frame)parentWindow, applianceName);
        else if( parentWindow instanceof Dialog )
            return new SaveFailureDialog((Dialog)parentWindow, applianceName);
        else
            return null;
    }

    private SaveFailureDialog(Dialog parentDialog, String applianceName) {
        super(parentDialog);
        init(applianceName);
    }
    private SaveFailureDialog(Frame parentFrame, String applianceName){
        super(parentFrame);
        init(applianceName);
    }

    private void init(String applianceName){
        if (null == applianceName) applianceName = "";
        applianceName = applianceName.trim();

        if (applianceName.length()>0) {
            setTitle(applianceName + " Warning");
        } else {
            setTitle("Warning");
        }

        messageJLabel.setText("<html><center>"
                              + ((applianceName.length() > 0) ? applianceName + " was u" : "U")
                              + "nable to save settings.<br>Please try again later.</center></html>");

        setVisible(true);
    }

}
