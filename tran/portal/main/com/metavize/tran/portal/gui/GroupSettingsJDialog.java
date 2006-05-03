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



public class GroupSettingsJDialog extends MConfigJDialog {

    private static final String NAME_TITLE     = "Group Settings";
    private static final String NAME_BOOKMARKS = "Links";
    private static final String NAME_HOME      = "Page Setup";

    private PortalGroup portalGroup;
        
    public static GroupSettingsJDialog factory(Window topLevelWindow, PortalGroup portalGroup){
	if( topLevelWindow instanceof Frame )
	    return new GroupSettingsJDialog((Frame)topLevelWindow, portalGroup);
	else if( topLevelWindow instanceof Dialog )
	    return new GroupSettingsJDialog((Dialog)topLevelWindow, portalGroup);
	else
	    return null;
    }

    public GroupSettingsJDialog(Dialog topLevelDialog, PortalGroup portalGroup){
	super(topLevelDialog);
	init(portalGroup);
    }

    public GroupSettingsJDialog(Frame topLevelFrame, PortalGroup portalGroup){
	super(topLevelFrame);
	init(portalGroup);
    }

    private void init(PortalGroup portalGroup){
	this.portalGroup = portalGroup;
    }
    
    protected Dimension getMinSize(){
	return new Dimension(640, 550);
    }

    protected void generateGui(){
        this.setTitle(NAME_TITLE + " for " + portalGroup.getName() );

        // BOOKMARKS //
	BookmarksJPanel bookmarksJPanel = new BookmarksJPanel(portalGroup);
	addRefreshable(NAME_BOOKMARKS, bookmarksJPanel);
	addSavable(NAME_BOOKMARKS, bookmarksJPanel);
        addTab(NAME_BOOKMARKS, null, bookmarksJPanel);

	// HOME //
	GroupHomeSettingsJPanel groupHomeSettingsJPanel = new GroupHomeSettingsJPanel(portalGroup);
	addRefreshable(NAME_HOME, groupHomeSettingsJPanel);
	addSavable(NAME_HOME, groupHomeSettingsJPanel);
	addScrollableTab( getMTabbedPane(), NAME_HOME, null, groupHomeSettingsJPanel, false, true);
    }

}
