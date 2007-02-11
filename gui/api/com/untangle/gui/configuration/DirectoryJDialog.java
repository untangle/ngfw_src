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

package com.untangle.gui.configuration;

import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;

import com.untangle.gui.util.*;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.widgets.editTable.*;

public class DirectoryJDialog extends MConfigJDialog {

    private static final String NAME_DIRECTORY_CONFIG  = "User Directory Config";
    private static final String NAME_LOCAL_DIRECTORY   = "Local Directory";
    private static final String NAME_REMOTE_ACTIVE_DIRECTORY   = "Remote Active Directory (AD) Server";
    
    public DirectoryJDialog( Frame parentFrame ) {
	super(parentFrame);
	setTitle(NAME_DIRECTORY_CONFIG);
	compoundSettings = new DirectoryCompoundSettings();
    }

    public DirectoryJDialog( Dialog parentDialog ) {
	super(parentDialog);
	setTitle(NAME_DIRECTORY_CONFIG);
	compoundSettings = new DirectoryCompoundSettings();
    }

    protected void generateGui(){
	// LOCAL DIRECTORY ////////
	DirectoryLocalJPanel directoryLocalJPanel = new DirectoryLocalJPanel();
	addTab(NAME_LOCAL_DIRECTORY, null, directoryLocalJPanel);
	addSavable(NAME_LOCAL_DIRECTORY, directoryLocalJPanel);
	addRefreshable(NAME_LOCAL_DIRECTORY, directoryLocalJPanel);

	// REMOTE ACTIVE DIRECTORY ////////
	DirectoryRemoteADJPanel directoryRemoteADJPanel = new DirectoryRemoteADJPanel();
	addScrollableTab(null, NAME_REMOTE_ACTIVE_DIRECTORY, null, directoryRemoteADJPanel, false, true);
	addSavable(NAME_REMOTE_ACTIVE_DIRECTORY, directoryRemoteADJPanel);
	addRefreshable(NAME_REMOTE_ACTIVE_DIRECTORY, directoryRemoteADJPanel);
    }

}
