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



package com.untangle.tran.reporting.gui;

import com.untangle.gui.util.Util;

import com.untangle.gui.transform.*;
import com.untangle.gui.pipeline.MPipelineJPanel;
import com.untangle.mvvm.tran.TransformContext;

import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;


public class MTransformControlsJPanel extends com.untangle.gui.transform.MTransformControlsJPanel{
    
    private static final String NAME_DIRECTORY = "IP Address to User Map";
    private static final String NAME_EMAIL_RECIPIENTS = "Recipients";
    private static final String NAME_VIEW = "View Reports";
    private static final String NAME_GENERATION_OPTIONS = "Generation Settings";

    public MTransformControlsJPanel(MTransformJPanel mTransformJPanel) {
        super(mTransformJPanel);
    }

    public void generateGui(){
	// LAUNCH BUTTON /////
	BrowserLaunchJPanel browserLaunchJPanel = new BrowserLaunchJPanel();
	addTab(NAME_VIEW, null, browserLaunchJPanel);

	// REPORTS EMAILING RECIPIENTS LIST /////
	EmailConfigJPanel emailConfigJPanel = new EmailConfigJPanel();
	addTab(NAME_EMAIL_RECIPIENTS, null, emailConfigJPanel);
	addSavable(NAME_EMAIL_RECIPIENTS, emailConfigJPanel);
	addRefreshable(NAME_EMAIL_RECIPIENTS, emailConfigJPanel);
	emailConfigJPanel.setSettingsChangedListener(this);
	
	// DIRECTORY ///////
	DirectoryConfigJPanel directoryConfigJPanel = new DirectoryConfigJPanel();
	addTab(NAME_DIRECTORY, null, directoryConfigJPanel);
	addSavable(NAME_DIRECTORY, directoryConfigJPanel);
	addRefreshable(NAME_DIRECTORY, directoryConfigJPanel);
	directoryConfigJPanel.setSettingsChangedListener(this);

	// GENERATION SETTINGS //
	GenerationConfigJPanel generationConfigJPanel = new GenerationConfigJPanel();
	addScrollableTab(null, NAME_GENERATION_OPTIONS, null, generationConfigJPanel, false, true);
	addSavable(NAME_GENERATION_OPTIONS, generationConfigJPanel);
	addRefreshable(NAME_GENERATION_OPTIONS, generationConfigJPanel);
	generationConfigJPanel.setSettingsChangedListener(this);
    }
    
}
