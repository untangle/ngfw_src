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



package com.untangle.tran.protofilter.gui;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.untangle.gui.transform.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.tran.protofilter.*;

public class MTransformControlsJPanel extends com.untangle.gui.transform.MTransformControlsJPanel{

    private static final String NAME_BLOCK_LIST = "Protocol List";
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


