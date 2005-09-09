 /*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */



package com.metavize.tran.reporting.gui;

import com.metavize.gui.util.Util;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.mvvm.tran.TransformContext;

import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;


public class MTransformControlsJPanel extends com.metavize.gui.transform.MTransformControlsJPanel{
    
    private static final String NAME_DIRECTORY = "IP Address to User Map";
    private static final String NAME_EMAIL = "Reports via Email";
    private static final String NAME_EMAIL_RECIPIENTS = "Recipients";
    private static final String NAME_EMAIL_GENERAL_SETTINGS = "General Settings";
    private static final String NAME_VIEW = "View Reports";

    public MTransformControlsJPanel(MTransformJPanel mTransformJPanel) {
        super(mTransformJPanel);
    }

    protected void generateGui(){

	// LAUNCH BUTTON /////
	BrowserLaunchJPanel browserLaunchJPanel = new BrowserLaunchJPanel();
	mTabbedPane.add(NAME_VIEW, browserLaunchJPanel);

	// REPORTS EMAILING ///////////
        JTabbedPane emailJTabbedPane = new JTabbedPane();
        emailJTabbedPane.setBorder(new EmptyBorder(7, 13, 13, 13));
        emailJTabbedPane.setFocusable(false);
        emailJTabbedPane.setFont(new java.awt.Font("Arial", 0, 11));
        emailJTabbedPane.setRequestFocusEnabled(false);
        this.mTabbedPane.add(NAME_EMAIL, emailJTabbedPane);

	// REPORTS EMAILING RECIPIENTS LIST /////
	EmailConfigJPanel emailConfigJPanel = new EmailConfigJPanel();
	emailJTabbedPane.add(NAME_EMAIL_RECIPIENTS, emailConfigJPanel);
	super.savableMap.put(NAME_EMAIL + " " + NAME_EMAIL_RECIPIENTS, emailConfigJPanel);
	super.refreshableMap.put(NAME_EMAIL + " " + NAME_EMAIL_RECIPIENTS, emailConfigJPanel);

	// REPORTS EMAILING GENERAL SETTINGS /////
	EmailGeneralConfigJPanel emailGeneralConfigJPanel = new EmailGeneralConfigJPanel();
	emailJTabbedPane.add(NAME_EMAIL_GENERAL_SETTINGS, emailGeneralConfigJPanel);
	super.savableMap.put(NAME_EMAIL + " " + NAME_EMAIL_GENERAL_SETTINGS, emailGeneralConfigJPanel);
	super.refreshableMap.put(NAME_EMAIL + " " + NAME_EMAIL_GENERAL_SETTINGS, emailGeneralConfigJPanel);
	
	// DIRECTORY ///////
	DirectoryConfigJPanel directoryConfigJPanel = new DirectoryConfigJPanel();
	mTabbedPane.add(NAME_DIRECTORY, directoryConfigJPanel);
	super.savableMap.put(NAME_DIRECTORY, directoryConfigJPanel);
	super.refreshableMap.put(NAME_DIRECTORY, directoryConfigJPanel);
        
    }
    
}
