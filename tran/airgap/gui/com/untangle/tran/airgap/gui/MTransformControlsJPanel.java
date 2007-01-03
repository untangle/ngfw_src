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


package com.untangle.tran.airgap.gui;

import com.untangle.gui.transform.*;
import com.untangle.gui.pipeline.MPipelineJPanel;
import com.untangle.mvvm.tran.TransformContext;

import javax.swing.*;
import java.awt.*;

public class MTransformControlsJPanel extends com.untangle.gui.transform.MTransformControlsJPanel{
    
    private static final String NAME_GENERAL_SETTINGS = "General Settings";
    private static final String NAME_LOG = "Event Log";
    private static final String NAME_SHIELD_PANEL = "Exception List";

    
    public MTransformControlsJPanel(MTransformJPanel mTransformJPanel) {
        super(mTransformJPanel);
    }
    
    public void generateGui() {
        // SHIELD CONFIGURATION SETTINGS /////
        ShieldNodeConfigurationJPanel shieldJPanel = new ShieldNodeConfigurationJPanel();
        addTab(NAME_SHIELD_PANEL, null, shieldJPanel );
	addSavable(NAME_SHIELD_PANEL, shieldJPanel );
	addRefreshable(NAME_SHIELD_PANEL, shieldJPanel );
	shieldJPanel.setSettingsChangedListener(this);

	// EVENT LOG //////////
	LogJPanel logJPanel = new LogJPanel(mTransformJPanel.getTransform(), this);
	addTab(NAME_LOG, null, logJPanel);
	addShutdownable(NAME_LOG, logJPanel);
    }
     
    
}
