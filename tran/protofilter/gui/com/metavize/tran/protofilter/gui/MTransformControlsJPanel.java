/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */



package com.metavize.tran.protofilter.gui;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.tran.protofilter.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.Vector;
import javax.swing.event.*;

public class MTransformControlsJPanel extends com.metavize.gui.transform.MTransformControlsJPanel{
    
    private static final String NAME_BLOCK_LIST = "Protocol Block List";
    private static final String NAME_LOG = "Event Log";
    
    public MTransformControlsJPanel(MTransformJPanel mTransformJPanel) {
        super(mTransformJPanel);
    }

    public void generateGui(){
	// BLOCK LIST /////
	ProtoConfigJPanel protoConfigJPanel = new ProtoConfigJPanel();
        addTab(NAME_BLOCK_LIST, null, protoConfigJPanel);
	addSavable(NAME_BLOCK_LIST, protoConfigJPanel);
	addRefreshable(NAME_BLOCK_LIST, protoConfigJPanel);
	protoConfigJPanel.setSettingsChangedListener(this);

        // EVENT LOG ///////
        LogJPanel logJPanel = new LogJPanel(mTransformJPanel.getTransform(), this);
        addTab(NAME_LOG, null, logJPanel);
	addShutdownable(NAME_LOG, logJPanel);
    }
    
}


