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
    
    
    public MTransformControlsJPanel(MTransformJPanel mTransformJPanel) throws Exception {
        super(mTransformJPanel);

	mTabbedPane.insertTab("IP Address to User Map", null, new DirectoryConfigJPanel(mTransformJPanel.getTransformContext()), null, 0);
	mTabbedPane.insertTab("Launch Web Browser to View Reports", null, new BrowserLaunchJPanel(), null, 0);
        
    }
    
}
