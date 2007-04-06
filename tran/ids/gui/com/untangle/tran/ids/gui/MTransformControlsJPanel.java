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



package com.untangle.tran.ids.gui;

import com.untangle.gui.transform.*;
import com.untangle.gui.pipeline.MPipelineJPanel;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.dialogs.*;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.Vector;
import javax.swing.event.*;

public class MTransformControlsJPanel extends com.untangle.gui.transform.MTransformControlsJPanel{
    
    private static final String NAME_STATUS = "Status";
    private static final String NAME_ADVANCED = "Advanced Settings";
    private static final String NAME_RULE_LIST = "Rule List";
    private static final String NAME_VARIABLE_LIST = "Variable List";
    private static final String NAME_LOG = "Event Log";
    
    public MTransformControlsJPanel(MTransformJPanel mTransformJPanel) {
        super(mTransformJPanel);
    }

    public void generateGui(){

        // STATUS
        IDSStatusJPanel idsStatusJPanel = new IDSStatusJPanel();
        addTab(NAME_STATUS, null, idsStatusJPanel);
        addRefreshable(NAME_STATUS, idsStatusJPanel);

        // ADVACED
        JTabbedPane advancedJTabbedPane = addTabbedPane(NAME_ADVANCED, null);
        
        // RULE LIST /////
        IDSConfigJPanel idsConfigJPanel = new IDSConfigJPanel();
        advancedJTabbedPane.addTab(NAME_RULE_LIST, null, idsConfigJPanel);
        addSavable(NAME_RULE_LIST, idsConfigJPanel);
        addRefreshable(NAME_RULE_LIST, idsConfigJPanel);
        idsConfigJPanel.setSettingsChangedListener(this);
        
        // VARIABLE LIST /////
        IDSVariableJPanel idsVariableJPanel = new IDSVariableJPanel();
        advancedJTabbedPane.addTab(NAME_VARIABLE_LIST, null, idsVariableJPanel);
        addSavable(NAME_VARIABLE_LIST, idsVariableJPanel);
        addRefreshable(NAME_VARIABLE_LIST, idsVariableJPanel);
        idsVariableJPanel.setSettingsChangedListener(this);
        
        // EVENT LOG ///////
        LogJPanel logJPanel = new LogJPanel(mTransformJPanel.getTransform(), this);
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


