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


