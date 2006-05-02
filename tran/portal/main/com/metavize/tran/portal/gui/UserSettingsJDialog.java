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

package com.metavize.tran.portal.gui;

import com.metavize.gui.widgets.dialogs.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;

import com.metavize.mvvm.portal.*;


import java.awt.Dimension;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.util.*;
import javax.swing.table.*;
import javax.swing.*;



public class UserSettingsJDialog extends MConfigJDialog {

    private static final String NAME_TITLE     = "User Settings";
    private static final String NAME_BOOKMARKS = "Bookmarks";
    private static final String NAME_HOME      = "Page Setup";

    private PortalUser portalUser;
        
    public static UserSettingsJDialog factory(Window topLevelWindow, PortalUser portalUser){
	if( topLevelWindow instanceof Frame )
	    return new UserSettingsJDialog((Frame)topLevelWindow, portalUser);
	else if( topLevelWindow instanceof Dialog )
	    return new UserSettingsJDialog((Dialog)topLevelWindow, portalUser);
	else
	    return null;
    }

    public UserSettingsJDialog(Dialog topLevelDialog, PortalUser portalUser){
	super(topLevelDialog);
	init(portalUser);
    }

    public UserSettingsJDialog(Frame topLevelFrame, PortalUser portalUser){
	super(topLevelFrame);
	init(portalUser);
    }

    private void init(PortalUser portalUser){
	this.portalUser = portalUser;
    }
    
    protected Dimension getMinSize(){
	return new Dimension(640, 550);
    }

    protected void generateGui(){
	String groupName;
	if( portalUser.getPortalGroup() != null )
	    groupName = portalUser.getPortalGroup().getName();
	else
	    groupName = "no group";
        this.setTitle(NAME_TITLE + " for " + portalUser.getUid() + " (" + groupName + ")");

        // BOOKMARKS //////
	BookmarksJPanel bookmarksJPanel = new BookmarksJPanel(portalUser);
	addRefreshable(NAME_BOOKMARKS, bookmarksJPanel);
	addSavable(NAME_BOOKMARKS, bookmarksJPanel);
        addTab(NAME_BOOKMARKS, null, bookmarksJPanel);

	// HOME //
	UserHomeSettingsJPanel userHomeSettingsJPanel = new UserHomeSettingsJPanel(portalUser);
	addRefreshable(NAME_HOME, userHomeSettingsJPanel);
	addSavable(NAME_HOME, userHomeSettingsJPanel);
	addScrollableTab(getMTabbedPane(), NAME_HOME, null, userHomeSettingsJPanel, false, true);
    }

}
