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


package com.untangle.tran.boxbackup.gui;

import com.untangle.gui.util.Util;

import com.untangle.gui.transform.*;
import com.untangle.gui.pipeline.MPipelineJPanel;
import com.untangle.mvvm.tran.TransformContext;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

import com.untangle.mvvm.addrbook.AddressBook;
import com.untangle.mvvm.addrbook.AddressBookSettings;
import com.untangle.mvvm.addrbook.AddressBookConfiguration;
import com.untangle.mvvm.addrbook.RepositorySettings;
import com.untangle.mvvm.addrbook.UserEntry;

import java.util.HashMap;
import java.util.List;

import com.untangle.mvvm.client.*;

import com.untangle.gui.test.*;


public class MTransformControlsJPanel extends com.untangle.gui.transform.MTransformControlsJPanel{

    private static final String NAME_LOG = "Event Log";

    public MTransformControlsJPanel(MTransformJPanel mTransformJPanel) {
        super(mTransformJPanel);        
    }

    public void generateGui(){
	// EVENT LOG //////////
	LogJPanel logJPanel = new LogJPanel(mTransformJPanel.getTransform(), this);
	addTab(NAME_LOG, null, logJPanel);
	addShutdownable(NAME_LOG, logJPanel);
    }

}
