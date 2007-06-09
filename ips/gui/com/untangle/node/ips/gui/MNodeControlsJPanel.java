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



package com.untangle.node.ips.gui;

import com.untangle.gui.node.*;
import com.untangle.gui.pipeline.MPipelineJPanel;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.dialogs.*;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.Vector;
import javax.swing.event.*;

public class MNodeControlsJPanel extends com.untangle.gui.node.MNodeControlsJPanel{
    
    private static final String NAME_STATUS = "Status";
    private static final String NAME_ADVANCED = "Advanced Settings";
    private static final String NAME_RULE_LIST = "Rule List";
    private static final String NAME_VARIABLE_LIST = "Variable List";
    private static final String NAME_LOG = "Event Log";
    
    public MNodeControlsJPanel(MNodeJPanel mNodeJPanel) {
        super(mNodeJPanel);
    }

    public void generateGui(){

        // STATUS
        IPSStatusJPanel ipsStatusJPanel = new IPSStatusJPanel();
        addTab(NAME_STATUS, null, ipsStatusJPanel);
        addRefreshable(NAME_STATUS, ipsStatusJPanel);

        // ADVACED
        JTabbedPane advancedJTabbedPane = addTabbedPane(NAME_ADVANCED, null);
        
        // RULE LIST /////
        IPSConfigJPanel ipsConfigJPanel = new IPSConfigJPanel();
        advancedJTabbedPane.addTab(NAME_RULE_LIST, null, ipsConfigJPanel);
        addSavable(NAME_RULE_LIST, ipsConfigJPanel);
        addRefreshable(NAME_RULE_LIST, ipsConfigJPanel);
        ipsConfigJPanel.setSettingsChangedListener(this);
        
        // VARIABLE LIST /////
        IPSVariableJPanel ipsVariableJPanel = new IPSVariableJPanel();
        advancedJTabbedPane.addTab(NAME_VARIABLE_LIST, null, ipsVariableJPanel);
        addSavable(NAME_VARIABLE_LIST, ipsVariableJPanel);
        addRefreshable(NAME_VARIABLE_LIST, ipsVariableJPanel);
        ipsVariableJPanel.setSettingsChangedListener(this);
        
        // EVENT LOG ///////
        LogJPanel logJPanel = new LogJPanel(mNodeJPanel.getNode(), this);
        addTab(NAME_LOG, null, logJPanel);
        addShutdownable(NAME_LOG, logJPanel);
    }


    private boolean shownOnce = false;
    
    public void settingsChanged(Object source){
        if(!shownOnce){
            MOneButtonJDialog.factory( (Window)this.getTopLevelAncestor(), "Intrusion Prevention",
                                       "You should only modify these rules if you are an experienced user"
                                       + " or you are instructed to do so.",
                                       "Intrusion Prevention Warning", "Warning");
            shownOnce = true;
        }
        super.settingsChanged(source);
    }
}


