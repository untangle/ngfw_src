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



package com.untangle.tran.portal.gui;

import com.untangle.mvvm.portal.*;

import com.untangle.mvvm.tran.TransformContext;
import com.untangle.mvvm.tran.IPMaddr;

import com.untangle.mvvm.addrbook.UserEntry;

import com.untangle.gui.transform.*;
import com.untangle.gui.pipeline.MPipelineJPanel;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.gui.util.*;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.Vector;
import java.util.List;
import javax.swing.event.*;
import javax.swing.border.EmptyBorder;

public class MTransformControlsJPanel extends com.untangle.gui.transform.MTransformControlsJPanel{

    private static final String NAME_USERS              = "Users";
    private static final String NAME_GROUPS             = "Groups";
    private static final String NAME_SETTINGS           = "Global Settings";
    private static final String NAME_SETTINGS_HOME      = "Page Setup";
    private static final String NAME_SETTINGS_RDP       = "Remote Desktop Bookmarks";
    private static final String NAME_SETTINGS_OTHER     = "Web, File Browser, VNC Bookmarks";
    private static final String NAME_STATUS             = "Active Users";
    private static final String NAME_LOG                = "Event Log";


    private List<Application> applications;
    private List<PortalLogin> loginList;
    
    public MTransformControlsJPanel(MTransformJPanel mTransformJPanel) {
        super(mTransformJPanel);
    }

    public void generateGui(){

	// USERS ///////////
	UserConfigJPanel userConfigJPanel = new UserConfigJPanel(this);
    addTab(NAME_USERS, null, userConfigJPanel);
    addSavable(NAME_USERS, userConfigJPanel);
	addRefreshable(NAME_USERS, userConfigJPanel);
	userConfigJPanel.setSettingsChangedListener(this);

	// GROUPS ///////////  THIS MUST BE AFTER USERS FOR PREVALIDATION REASONS
	GroupConfigJPanel groupConfigJPanel = new GroupConfigJPanel(this);
    addTab(NAME_GROUPS, null, groupConfigJPanel);
	addSavable(NAME_GROUPS, groupConfigJPanel);
	addRefreshable(NAME_GROUPS, groupConfigJPanel);
	groupConfigJPanel.setSettingsChangedListener(this);

	// GLOBAL SETTINGS //
	JTabbedPane settingsJTabbedPane = addTabbedPane(NAME_SETTINGS, null);

	// OTHER BOOKMARKS //
	BookmarksJPanel otherBookmarksJPanel = new BookmarksJPanel(null, applications, "OTHER");
	settingsJTabbedPane.addTab(NAME_SETTINGS_OTHER, null, otherBookmarksJPanel);
	addSavable(NAME_SETTINGS_OTHER, otherBookmarksJPanel);
	addRefreshable(NAME_SETTINGS_OTHER, otherBookmarksJPanel);
	otherBookmarksJPanel.setSettingsChangedListener(this);

	// RDP BOOKMARKS //
	BookmarksJPanel rdpBookmarksJPanel = new BookmarksJPanel(null, applications, "RDP");
	settingsJTabbedPane.addTab(NAME_SETTINGS_RDP, null, rdpBookmarksJPanel);
	addSavable(NAME_SETTINGS_RDP, rdpBookmarksJPanel);
	addRefreshable(NAME_SETTINGS_RDP, rdpBookmarksJPanel);
	rdpBookmarksJPanel.setSettingsChangedListener(this);

	// GLOBAL PAGE SETTINGS //
	GlobalHomeSettingsJPanel globalHomeSettingsJPanel = new GlobalHomeSettingsJPanel();
	addScrollableTab(settingsJTabbedPane, NAME_SETTINGS_HOME, null, globalHomeSettingsJPanel, false, true);
	addSavable(NAME_SETTINGS_HOME, globalHomeSettingsJPanel);
	addRefreshable(NAME_SETTINGS_HOME, globalHomeSettingsJPanel);
	globalHomeSettingsJPanel.setSettingsChangedListener(this);

    // STATUS ////////
	KickUserJPanel kickUserJPanel = new KickUserJPanel(this);
    kickUserJPanel.setMTransformJPanel(mTransformJPanel);
    addTab(NAME_STATUS, null, kickUserJPanel);
	addSavable(NAME_STATUS, kickUserJPanel);
	addRefreshable(NAME_STATUS, kickUserJPanel);

 	// EVENT LOG ///////
	LogJPanel logJPanel = new LogJPanel(mTransformJPanel.getTransform(), this);
    addTab(NAME_LOG, null, logJPanel);
    addShutdownable(NAME_LOG, logJPanel);
    }

    List<PortalLogin> getLoginList(){
        return loginList;
    }

    public void refreshAll() throws Exception {
        applications = Util.getRemotePortalManager().applicationManager().getApplications();
        loginList = Util.getRemotePortalManager().getActiveLogins();
        super.refreshAll();
    }

}
