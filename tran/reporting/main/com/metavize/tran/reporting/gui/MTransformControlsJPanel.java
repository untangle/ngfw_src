/*
 * 
 *
 * Created on March 25, 2004, 6:11 PM
 */

package com.metavize.tran.reporting.gui;

import com.metavize.gui.util.Util;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.mvvm.tran.TransformContext;


public class MTransformControlsJPanel extends com.metavize.gui.transform.MTransformControlsJPanel{
    
    private static final String NAME_DIRECTORY = "IP Address to User Map";
    private static final String NAME_EMAIL = "Reports via Email";
    private static final String NAME_VIEW = "View Reports";

    public MTransformControlsJPanel(MTransformJPanel mTransformJPanel) {
        super(mTransformJPanel);
    }

    protected void generateGui(){
	// DIRECTORY ///////
	DirectoryConfigJPanel directoryConfigJPanel = new DirectoryConfigJPanel();
	mTabbedPane.insertTab(NAME_DIRECTORY, null, directoryConfigJPanel, null, 0);
	super.savableMap.put(NAME_DIRECTORY, directoryConfigJPanel);
	super.refreshableMap.put(NAME_DIRECTORY, directoryConfigJPanel);

	// EMAIL LIST /////
	EmailConfigJPanel emailConfigJPanel = new EmailConfigJPanel();
	mTabbedPane.insertTab(NAME_EMAIL, null, emailConfigJPanel, null, 0);
	super.savableMap.put(NAME_EMAIL, emailConfigJPanel);
	super.refreshableMap.put(NAME_EMAIL, emailConfigJPanel);
	
	// LAUNCH BUTTON /////
	mTabbedPane.insertTab(NAME_VIEW, null, new BrowserLaunchJPanel(), null, 0);
        
    }
    
}
