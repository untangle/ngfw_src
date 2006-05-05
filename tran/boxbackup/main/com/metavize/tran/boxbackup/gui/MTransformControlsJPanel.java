/*
 * Copyright (c) 2003,2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */


package com.metavize.tran.boxbackup.gui;

import com.metavize.gui.util.Util;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.mvvm.tran.TransformContext;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

import com.metavize.mvvm.addrbook.AddressBook;
import com.metavize.mvvm.addrbook.AddressBookSettings;
import com.metavize.mvvm.addrbook.AddressBookConfiguration;
import com.metavize.mvvm.addrbook.RepositorySettings;
import com.metavize.mvvm.addrbook.UserEntry;

import java.util.HashMap;
import java.util.List;

import com.metavize.mvvm.client.*;

import com.metavize.gui.test.*;


public class MTransformControlsJPanel extends com.metavize.gui.transform.MTransformControlsJPanel{

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
