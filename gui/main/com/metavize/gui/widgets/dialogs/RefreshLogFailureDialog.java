/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.gui.widgets.dialogs;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;

final public class RefreshLogFailureDialog extends MOneButtonJDialog {

    public static RefreshLogFailureDialog factory(Window parentWindow, String applianceName){
	if( parentWindow instanceof Dialog )
	    return new RefreshLogFailureDialog((Dialog) parentWindow, applianceName);
	else if( parentWindow instanceof Frame )
	    return new RefreshLogFailureDialog((Frame) parentWindow, applianceName);
	else
	    return null;
    }
    
    private RefreshLogFailureDialog(Dialog parentDialog, String applianceName) {
	super(parentDialog);
	init(applianceName);
    }
    private RefreshLogFailureDialog(Frame parentFrame, String applianceName){
	super(parentFrame);
	init(applianceName);
    }

    private void init(String applianceName){
        setTitle(applianceName + " Warning");
        messageJLabel.setText("<html><center>" + applianceName + " was unable to properly refresh its event log.<br>Please try again later.</center></html>");
        setVisible(true);
    }
    
}
