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


package com.untangle.node.shield.gui;

import java.awt.*;
import javax.swing.*;

import com.untangle.gui.node.*;

public class MNodeControlsJPanel extends com.untangle.gui.node.MNodeControlsJPanel{

    private static final String NAME_GENERAL_SETTINGS = "General Settings";
    private static final String NAME_LOG = "Event Log";
    private static final String NAME_SHIELD_PANEL = "Exception List";


    public MNodeControlsJPanel(MNodeJPanel mNodeJPanel) {
        super(mNodeJPanel);
    }

    public void generateGui() {
        // SHIELD CONFIGURATION SETTINGS /////
        ShieldNodeConfigurationJPanel shieldJPanel = new ShieldNodeConfigurationJPanel();
        addTab(NAME_SHIELD_PANEL, null, shieldJPanel );
        addSavable(NAME_SHIELD_PANEL, shieldJPanel );
        addRefreshable(NAME_SHIELD_PANEL, shieldJPanel );
        shieldJPanel.setSettingsChangedListener(this);

        // EVENT LOG //////////
        LogJPanel logJPanel = new LogJPanel(mNodeJPanel.getNode(), this);
        addTab(NAME_LOG, null, logJPanel);
        addShutdownable(NAME_LOG, logJPanel);
    }


}
