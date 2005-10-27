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


package com.metavize.tran.test.gui;

import com.metavize.gui.util.Util;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.mvvm.tran.TransformContext;


public class MTransformControlsJPanel extends com.metavize.gui.transform.MTransformControlsJPanel{
    
    private static final String NAME_SOME_LIST = "Some List";
    private static final String NAME_LOG = "Event Log";
    
    public MTransformControlsJPanel(MTransformJPanel mTransformJPanel) {
        super(mTransformJPanel);        
    }

    protected void generateGui(){
	// SOME LIST //
	javax.swing.JPanel someJPanel = new javax.swing.JPanel();
	super.mTabbedPane.addTab(NAME_SOME_LIST, null, someJPanel);
	//super.savableMap.put(NAME_SOME_LIST, someJPanel);
	//super.refreshableMap.put(NAME_SOME_LIST, someJPanel);

	// EVENT LOG /////
	//LogJPanel logJPanel = new LogJPanel(mTransformJPanel.getTransformContext().transform(), this);
	//super.mTabbedPane.addTab(NAME_LOG, null, logJPanel);
	//super.shutdownableMap.put(NAME_LOG, logJPanel);
    }
    
}
