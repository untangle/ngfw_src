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

    protected void generateGui(){

	// LAUNCH BUTTON /////
	BrowserLaunchJPanel browserLaunchJPanel = new BrowserLaunchJPanel();
	mTabbedPane.add(NAME_VIEW, browserLaunchJPanel);

	// REPORTS EMAILING RECIPIENTS LIST /////
	EmailConfigJPanel emailConfigJPanel = new EmailConfigJPanel();
	mTabbedPane.add(NAME_EMAIL_RECIPIENTS, emailConfigJPanel);
	super.savableMap.put(NAME_EMAIL_RECIPIENTS, emailConfigJPanel);
	super.refreshableMap.put(NAME_EMAIL_RECIPIENTS, emailConfigJPanel);
	
	// DIRECTORY ///////
	DirectoryConfigJPanel directoryConfigJPanel = new DirectoryConfigJPanel();
	mTabbedPane.add(NAME_DIRECTORY, directoryConfigJPanel);
	super.savableMap.put(NAME_DIRECTORY, directoryConfigJPanel);
	super.refreshableMap.put(NAME_DIRECTORY, directoryConfigJPanel);
        
    }
    
}
