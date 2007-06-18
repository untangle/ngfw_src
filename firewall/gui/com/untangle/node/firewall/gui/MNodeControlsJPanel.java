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



package com.untangle.node.firewall.gui;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.untangle.gui.node.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.node.firewall.*;

public class MNodeControlsJPanel extends com.untangle.gui.node.MNodeControlsJPanel{

    private static final String NAME_BLOCK_LIST = "Rule List";
    private static final String NAME_GENERAL_SETTINGS = "General Settings";
    private static final String NAME_LOG = "Event Log";

    protected Dimension MIN_SIZE = new Dimension(640, 480);
    protected Dimension MAX_SIZE = new Dimension(640, 1200);

    public MNodeControlsJPanel(MNodeJPanel mNodeJPanel) {
        super(mNodeJPanel);
    }

    public void generateGui(){
        // SETUP FIREWALL
        BlockJPanel blockJPanel = new BlockJPanel();
        addTab(NAME_BLOCK_LIST, null, blockJPanel);
        addSavable(NAME_BLOCK_LIST, blockJPanel);
        addRefreshable(NAME_BLOCK_LIST, blockJPanel);
        blockJPanel.setSettingsChangedListener(this);

        // SETUP GENERAL SETTINGS
        SettingsJPanel settingsJPanel = new SettingsJPanel();
        JScrollPane settingsJScrollPane = new JScrollPane( settingsJPanel );
        settingsJScrollPane.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
        settingsJScrollPane.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );
        addTab(NAME_GENERAL_SETTINGS, null, settingsJScrollPane );
        addSavable(NAME_GENERAL_SETTINGS, settingsJPanel);
        addRefreshable(NAME_GENERAL_SETTINGS, settingsJPanel);
        settingsJPanel.setSettingsChangedListener(this);

        // EVENT LOG
        LogJPanel logJPanel = new LogJPanel(mNodeJPanel.getNode(), this);
        addTab(NAME_LOG, null, logJPanel);
        addShutdownable(NAME_LOG, logJPanel);
    }

}


