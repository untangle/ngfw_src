/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
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
