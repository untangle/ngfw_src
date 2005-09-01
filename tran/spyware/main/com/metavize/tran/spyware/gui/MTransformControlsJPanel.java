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



package com.metavize.tran.spyware.gui;

import com.metavize.mvvm.tran.TransformContext;

import com.metavize.mvvm.tran.IPMaddr;
import com.metavize.tran.spyware.*;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.Vector;
import javax.swing.event.*;
import javax.swing.border.EmptyBorder;

public class MTransformControlsJPanel extends com.metavize.gui.transform.MTransformControlsJPanel{

    private static final String NAME_BLOCK = "Block Lists";
    private static final String NAME_BLOCK_ACTIVEX = "ActiveX List";
    private static final String NAME_BLOCK_SPYWARE = "Subnet List";
    private static final String NAME_BLOCK_COOKIE = "Cookie List";
    private static final String NAME_BLOCK_URL = "URL List";
    private static final String NAME_SETTINGS = "General Settings";
    private static final String NAME_LOG = "Event Log";
    
    public MTransformControlsJPanel(MTransformJPanel mTransformJPanel) {
        super(mTransformJPanel);
    }

    protected void generateGui(){

	// BLOCK LISTS ///////////
        JTabbedPane blockJTabbedPane = new JTabbedPane();
        blockJTabbedPane.setBorder(new EmptyBorder(7, 13, 13, 13));
        blockJTabbedPane.setFocusable(false);
        blockJTabbedPane.setFont(new java.awt.Font("Arial", 0, 11));
        blockJTabbedPane.setRequestFocusEnabled(false);
        super.mTabbedPane.addTab(NAME_BLOCK, null, blockJTabbedPane);

	// COOKIES //////////////
	CookieConfigJPanel cookieConfigJPanel = new CookieConfigJPanel();
        blockJTabbedPane.addTab(NAME_BLOCK_COOKIE, null, cookieConfigJPanel);
	super.savableMap.put(NAME_BLOCK + " " + NAME_BLOCK_COOKIE, cookieConfigJPanel);
	super.refreshableMap.put(NAME_BLOCK + " " + NAME_BLOCK_COOKIE, cookieConfigJPanel);

	// SPYWARE ///////////////
	SpywareConfigJPanel spywareConfigJPanel = new SpywareConfigJPanel();
        blockJTabbedPane.addTab(NAME_BLOCK_SPYWARE, null, spywareConfigJPanel);
	super.savableMap.put(NAME_BLOCK + " " + NAME_BLOCK_SPYWARE, spywareConfigJPanel);
	super.refreshableMap.put(NAME_BLOCK + " " + NAME_BLOCK_SPYWARE, spywareConfigJPanel);

	// ACTIVEX ///////////////
	ActiveXConfigJPanel activeXConfigJPanel = new ActiveXConfigJPanel();
        blockJTabbedPane.addTab(NAME_BLOCK_ACTIVEX, null, activeXConfigJPanel);
	super.savableMap.put(NAME_BLOCK + " " + NAME_BLOCK_ACTIVEX, activeXConfigJPanel);
	super.refreshableMap.put(NAME_BLOCK + " " + NAME_BLOCK_ACTIVEX, activeXConfigJPanel);
        
        // URL ///////////////
	UrlConfigJPanel urlConfigJPanel = new UrlConfigJPanel();
        blockJTabbedPane.addTab(NAME_BLOCK_URL, null, urlConfigJPanel);
	super.savableMap.put(NAME_BLOCK + " " + NAME_BLOCK_URL, urlConfigJPanel);
	super.refreshableMap.put(NAME_BLOCK + " " + NAME_BLOCK_URL, urlConfigJPanel);

        // GENERAL SETTINGS ////////
	GeneralConfigJPanel generalConfigJPanel = new GeneralConfigJPanel();
        super.mTabbedPane.addTab(NAME_SETTINGS, null, generalConfigJPanel);
	super.savableMap.put(NAME_SETTINGS, generalConfigJPanel);
	super.refreshableMap.put(NAME_SETTINGS, generalConfigJPanel);

 	// EVENT LOG ///////
	LogJPanel logJPanel = new LogJPanel(mTransformJPanel.getTransformContext().transform(), this);
        super.mTabbedPane.addTab(NAME_LOG, null, logJPanel);
        super.shutdownableMap.put(NAME_LOG, logJPanel);
    }
}


