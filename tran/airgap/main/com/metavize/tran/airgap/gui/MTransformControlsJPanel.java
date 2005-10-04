/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */


package com.metavize.tran.airgap.gui;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.mvvm.tran.TransformContext;

import javax.swing.*;
import java.awt.*;

public class MTransformControlsJPanel extends com.metavize.gui.transform.MTransformControlsJPanel{
    
    private static final String NAME_GENERAL_SETTINGS = "General Settings";
    private static final String NAME_LOG = "Event Log";
    private static final String NAME_SHIELD_PANEL = "Exception List";

    
    public MTransformControlsJPanel(MTransformJPanel mTransformJPanel) {
        super(mTransformJPanel);
    }
    
    protected void generateGui() {
        // SHIELD CONFIGURATION SETTINGS /////
        ShieldNodeConfigurationJPanel shieldJPanel = new ShieldNodeConfigurationJPanel();
        super.mTabbedPane.addTab(NAME_SHIELD_PANEL, null, shieldJPanel );
	super.savableMap.put(NAME_SHIELD_PANEL, shieldJPanel );
	super.refreshableMap.put(NAME_SHIELD_PANEL, shieldJPanel );

	// EVENT LOG //////////
	LogJPanel logJPanel = new LogJPanel(mTransformJPanel.getTransformContext().transform(), this);
	super.mTabbedPane.addTab(NAME_LOG, null, logJPanel);
	super.shutdownableMap.put(NAME_LOG, logJPanel);
    }
     
    
}
