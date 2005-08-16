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
    private static final String NAME_LOG = "Event Log";
    
    public MTransformControlsJPanel(MTransformJPanel mTransformJPanel) {
        super(mTransformJPanel);
    }

    protected void generateGui(){
	// RULE LIST /////
	IDSConfigJPanel idsConfigJPanel = new IDSConfigJPanel();
        this.mTabbedPane.addTab(NAME_RULE_LIST, null, idsConfigJPanel);
	super.savableMap.put(NAME_RULE_LIST, idsConfigJPanel);
	super.refreshableMap.put(NAME_RULE_LIST, idsConfigJPanel);

        // EVENT LOG ///////
        LogJPanel logJPanel = new LogJPanel(mTransformJPanel.getTransformContext().transform(), this);
        this.mTabbedPane.addTab(NAME_LOG, null, logJPanel);
    }
    
}


