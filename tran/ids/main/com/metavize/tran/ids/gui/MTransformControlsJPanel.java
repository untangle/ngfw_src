/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */



package com.metavize.tran.ids.gui;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.Vector;
import javax.swing.event.*;

public class MTransformControlsJPanel extends com.metavize.gui.transform.MTransformControlsJPanel{
    
    private static final String NAME_RULE_LIST = "Rule List";
    private static final String NAME_VARIABLE_LIST = "Variable List";
    private static final String NAME_LOG = "Event Log";
    
    public MTransformControlsJPanel(MTransformJPanel mTransformJPanel) {
        super(mTransformJPanel);
    }

    public void generateGui(){
	// RULE LIST /////
	IDSConfigJPanel idsConfigJPanel = new IDSConfigJPanel();
        addTab(NAME_RULE_LIST, null, idsConfigJPanel);
	addSavable(NAME_RULE_LIST, idsConfigJPanel);
	addRefreshable(NAME_RULE_LIST, idsConfigJPanel);
	idsConfigJPanel.setSettingsChangedListener(this);

	// VARIABLE LIST /////
	IDSVariableJPanel idsVariableJPanel = new IDSVariableJPanel();
        addTab(NAME_VARIABLE_LIST, null, idsVariableJPanel);
	addSavable(NAME_VARIABLE_LIST, idsVariableJPanel);
	addRefreshable(NAME_VARIABLE_LIST, idsVariableJPanel);
	idsVariableJPanel.setSettingsChangedListener(this);

        // EVENT LOG ///////
        LogJPanel logJPanel = new LogJPanel(mTransformJPanel.getTransform(), this);
        addTab(NAME_LOG, null, logJPanel);
	addShutdownable(NAME_LOG, logJPanel);
    }
    
}


