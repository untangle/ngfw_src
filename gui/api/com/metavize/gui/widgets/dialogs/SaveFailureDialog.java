/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle Networks, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.gui.widgets.dialogs;

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
	setTitle(applianceName + " Warning");
        messageJLabel.setText("<html><center>" + applianceName + " was unable to save settings.<br>Please try again later.</center></html>");
        setVisible(true);
    }
    
}
