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

    private static final String NAME_USERS    = "Users";
    private static final String NAME_GROUPS   = "Groups";
    private static final String NAME_SETTINGS = "General Settings";
    private static final String NAME_STATUS   = "Status";
    private static final String NAME_LOG      = "Event Log";
    
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
	/*
	// GROUPS ///////////
	GroupConfigJPanel groupConfigJPanel = new GroupConfigJPanel();
        addTab(NAME_GROUPS, null, groupConfigJPanel);
	addSavable(NAME_GROUPS, groupConfigJPanel);
	addRefreshable(NAME_GROUPS, groupConfigJPanel);
	groupConfigJPanel.setSettingsChangedListener(this);

        // GENERAL SETTINGS ////////
	GeneralConfigJPanel generalConfigJPanel = new GeneralConfigJPanel();
        addTab(NAME_SETTINGS, null, generalConfigJPanel);
	addSavable(NAME_SETTINGS, generalConfigJPanel);
	addRefreshable(NAME_SETTINGS, generalConfigJPanel);
	generalConfigJPanel.setSettingsChangedListener(this);
	*/
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


