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


package com.untangle.tran.test.gui;


import java.awt.*;
import java.awt.event.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;

import com.untangle.gui.test.*;
import com.untangle.gui.transform.*;
import com.untangle.mvvm.client.*;


public class MTransformControlsJPanel extends com.untangle.gui.transform.MTransformControlsJPanel{

    private static final String NAME_SOME_LIST = "Some List";
    private static final String NAME_LOG = "Event Log";

    public MTransformControlsJPanel(MTransformJPanel mTransformJPanel) {
        super(mTransformJPanel);
    }

    public void generateGui(){
        // SOME LIST //
        javax.swing.JPanel someJPanel = new javax.swing.JPanel();
        addTab(NAME_SOME_LIST, null, someJPanel);

        addTab("Address Book Test", null, new AddressBookTestPanel());

        //super.savableMap.put(NAME_SOME_LIST, someJPanel);
        //super.refreshableMap.put(NAME_SOME_LIST, someJPanel);

        // EVENT LOG /////
        //LogJPanel logJPanel = new LogJPanel(mTransformJPanel.getTransformContext().transform(), this);
        //addTab(NAME_LOG, null, logJPanel);
        //addShutdownable(NAME_LOG, logJPanel);
    }

}
