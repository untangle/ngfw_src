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

package com.metavize.gui.configuration;

import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;

import com.metavize.gui.util.*;
import com.metavize.gui.widgets.dialogs.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.mvvm.NetworkingConfiguration;

public class DirectoryJDialog extends MConfigJDialog {

    private static final String NAME_DIRECTORY_CONFIG  = "User Directory Config";
    private static final String NAME_LOCAL_DIRECTORY   = "Local LDAP Directory";
    private static final String NAME_REMOTE_ACTIVE_DIRECTORY   = "Remote Active Directory (AD) Server";
    
    public DirectoryJDialog( Frame parentFrame ) {
	super(parentFrame);
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
	addTab(NAME_REMOTE_ACTIVE_DIRECTORY, null, directoryRemoteADJPanel);
	addSavable(NAME_REMOTE_ACTIVE_DIRECTORY, directoryRemoteADJPanel);
	addRefreshable(NAME_REMOTE_ACTIVE_DIRECTORY, directoryRemoteADJPanel);
    }

}
