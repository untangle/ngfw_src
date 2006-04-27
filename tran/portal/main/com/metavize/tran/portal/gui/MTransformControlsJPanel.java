/*
 * Copyright (c) 2003,2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */



package com.metavize.tran.portal.gui;

import com.metavize.mvvm.tran.TransformContext;

import com.metavize.mvvm.tran.IPMaddr;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.Vector;
import javax.swing.event.*;
import javax.swing.border.EmptyBorder;

public class MTransformControlsJPanel extends com.metavize.gui.transform.MTransformControlsJPanel{

    private static final String NAME_USERS              = "Users";
    private static final String NAME_GROUPS             = "Groups";
    private static final String NAME_SETTINGS           = "Global Settings";
    private static final String NAME_SETTINGS_HOME      = "Home Page";
    private static final String NAME_SETTINGS_BOOKMARKS = "Bookmarks";
    private static final String NAME_STATUS             = "Status";
    private static final String NAME_LOG                = "Event Log";
    
    public MTransformControlsJPanel(MTransformJPanel mTransformJPanel) {
        super(mTransformJPanel);
    }

    public void generateGui(){

	// USERS ///////////
	UserConfigJPanel userConfigJPanel = new UserConfigJPanel();
        addTab(NAME_USERS, null, userConfigJPanel);
	addSavable(NAME_USERS, userConfigJPanel);
	addRefreshable(NAME_USERS, userConfigJPanel);
	userConfigJPanel.setSettingsChangedListener(this);

	// GROUPS ///////////
	GroupConfigJPanel groupConfigJPanel = new GroupConfigJPanel();
        addTab(NAME_GROUPS, null, groupConfigJPanel);
	addSavable(NAME_GROUPS, groupConfigJPanel);
	addRefreshable(NAME_GROUPS, groupConfigJPanel);
	groupConfigJPanel.setSettingsChangedListener(this);

        // GLOBAL SETTINGS ////////
	JTabbedPane globalJTabbedPane = addTabbedPane(NAME_SETTINGS, null);

	// GLOBAL HOME SETTINGS //
	GlobalHomeSettingsJPanel globalHomeSettingsJPanel = new GlobalHomeSettingsJPanel();
	globalJTabbedPane.addTab(NAME_SETTINGS_HOME, null, globalHomeSettingsJPanel);
	addSavable(NAME_SETTINGS_HOME, globalHomeSettingsJPanel);
	addRefreshable(NAME_SETTINGS_HOME, globalHomeSettingsJPanel);
	globalHomeSettingsJPanel.setSettingsChangedListener(this);

	// GLOBAL BOOKMARKS //
	BookmarksJPanel bookmarksJPanel = new BookmarksJPanel(null);
	globalJTabbedPane.addTab(NAME_SETTINGS_BOOKMARKS, null, bookmarksJPanel);
	addSavable(NAME_SETTINGS_BOOKMARKS, bookmarksJPanel);
	addRefreshable(NAME_SETTINGS_BOOKMARKS, bookmarksJPanel);
	bookmarksJPanel.setSettingsChangedListener(this);

	/*
        // STATUS ////////
	StatusJPanel statusJPanel = new StatusJPanel();
        addTab(NAME_STATUS, null, statusJPanel);
	addSavable(NAME_STATUS, statusJPanel);
	addRefreshable(NAME_STATUS, statusJPanel);

 	// EVENT LOG ///////
	LogJPanel logJPanel = new LogJPanel(mTransformJPanel.getTransform(), this);
        addTab(NAME_LOG, null, logJPanel);
        addShutdownable(NAME_LOG, logJPanel);
	*/
    }
}


