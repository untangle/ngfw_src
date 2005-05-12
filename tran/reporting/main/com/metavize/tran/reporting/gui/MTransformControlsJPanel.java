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

    public MTransformControlsJPanel(MTransformJPanel mTransformJPanel) {
        super(mTransformJPanel);
    }

    protected void generateGui(){
	// DIRECTORY ///////
	DirectoryConfigJPanel directoryConfigJPanel = new DirectoryConfigJPanel();
	mTabbedPane.insertTab(NAME_DIRECTORY, null, directoryConfigJPanel, null, 0);
	super.savableMap.put(NAME_DIRECTORY, directoryConfigJPanel);
	super.refreshableMap.put(NAME_DIRECTORY, directoryConfigJPanel);

	// LAUNCH BUTTON /////
	mTabbedPane.insertTab("Launch Web Browser to View Reports", null, new BrowserLaunchJPanel(), null, 0);
        
    }
    
}
