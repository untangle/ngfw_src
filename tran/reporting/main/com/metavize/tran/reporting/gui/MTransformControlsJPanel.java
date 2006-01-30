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
    private static final String NAME_EMAIL_RECIPIENTS = "Email Report Recipients";
    private static final String NAME_VIEW = "View Reports";

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
	
	// DIRECTORY ///////
	DirectoryConfigJPanel directoryConfigJPanel = new DirectoryConfigJPanel();
	addTab(NAME_DIRECTORY, null, directoryConfigJPanel);
	addSavable(NAME_DIRECTORY, directoryConfigJPanel);
	addRefreshable(NAME_DIRECTORY, directoryConfigJPanel);        
    }
    
}
