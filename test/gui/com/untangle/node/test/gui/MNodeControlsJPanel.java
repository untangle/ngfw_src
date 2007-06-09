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


package com.untangle.node.test.gui;


import java.awt.*;
import java.awt.event.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;

import com.untangle.gui.test.*;
import com.untangle.gui.node.*;
import com.untangle.uvm.client.*;


public class MNodeControlsJPanel extends com.untangle.gui.node.MNodeControlsJPanel{

    private static final String NAME_SOME_LIST = "Some List";
    private static final String NAME_LOG = "Event Log";

    public MNodeControlsJPanel(MNodeJPanel mNodeJPanel) {
        super(mNodeJPanel);
    }

    public void generateGui(){
        // SOME LIST //
        javax.swing.JPanel someJPanel = new javax.swing.JPanel();
        addTab(NAME_SOME_LIST, null, someJPanel);

        addTab("Address Book Test", null, new AddressBookTestPanel());

        //super.savableMap.put(NAME_SOME_LIST, someJPanel);
        //super.refreshableMap.put(NAME_SOME_LIST, someJPanel);

        // EVENT LOG /////
        //LogJPanel logJPanel = new LogJPanel(mNodeJPanel.getNodeContext().node(), this);
        //addTab(NAME_LOG, null, logJPanel);
        //addShutdownable(NAME_LOG, logJPanel);
    }

}
