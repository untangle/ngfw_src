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
	// DIRECTORY ///////
	DirectoryConfigJPanel directoryConfigJPanel = new DirectoryConfigJPanel();
	mTabbedPane.insertTab(NAME_DIRECTORY, null, directoryConfigJPanel, null, 0);
	super.savableMap.put(NAME_DIRECTORY, directoryConfigJPanel);
	super.refreshableMap.put(NAME_DIRECTORY, directoryConfigJPanel);

	// REPORTS EMAILING ///////////
        JTabbedPane emailJTabbedPane = new JTabbedPane();
        emailJTabbedPane.setBorder(new EmptyBorder(7, 13, 13, 13));
        emailJTabbedPane.setFocusable(false);
        emailJTabbedPane.setFont(new java.awt.Font("Arial", 0, 11));
        emailJTabbedPane.setRequestFocusEnabled(false);
        this.mTabbedPane.insertTab(NAME_EMAIL, null, emailJTabbedPane, null, 0);        

	// REPORTS EMAILING GENERAL SETTINGS /////
	EmailGeneralConfigJPanel emailGeneralConfigJPanel = new EmailGeneralConfigJPanel();
	emailJTabbedPane.insertTab(NAME_EMAIL_GENERAL_SETTINGS, null, emailGeneralConfigJPanel, null, 0);
	super.savableMap.put(NAME_EMAIL + " " + NAME_EMAIL_GENERAL_SETTINGS, emailGeneralConfigJPanel);
	super.refreshableMap.put(NAME_EMAIL + " " + NAME_EMAIL_GENERAL_SETTINGS, emailGeneralConfigJPanel);

	// REPORTS EMAILING RECIPIENTS LIST /////
	EmailConfigJPanel emailConfigJPanel = new EmailConfigJPanel();
	emailJTabbedPane.insertTab(NAME_EMAIL_RECIPIENTS, null, emailConfigJPanel, null, 0);
	super.savableMap.put(NAME_EMAIL + " " + NAME_EMAIL_RECIPIENTS, emailConfigJPanel);
	super.refreshableMap.put(NAME_EMAIL + " " + NAME_EMAIL_RECIPIENTS, emailConfigJPanel);
	
	// LAUNCH BUTTON /////
	mTabbedPane.insertTab(NAME_VIEW, null, new BrowserLaunchJPanel(), null, 0);

	// SET TAB SELECTIONS /////////
        emailJTabbedPane.setSelectedIndex(0);

        
    }
    
}
