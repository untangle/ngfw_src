/*
 * $HeadURL:$
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


