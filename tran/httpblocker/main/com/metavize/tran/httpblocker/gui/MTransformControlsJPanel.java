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
package com.metavize.tran.httpblocker.gui;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.mvvm.tran.TransformContext;

import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;

public class MTransformControlsJPanel extends com.metavize.gui.transform.MTransformControlsJPanel{
    
    private static final String NAME_PASS = "Pass Lists";
    private static final String NAME_PASS_CLIENTS = "Clients";
    private static final String NAME_PASS_URLS = "URLs";
    private static final String NAME_BLOCK = "Block Lists";
    private static final String NAME_BLOCK_FILE_EXTENSIONS = "File Extensions";
    private static final String NAME_BLOCK_MIME_TYPES = "MIME Types";
    private static final String NAME_BLOCK_URLS = "URLs";
    private static final String NAME_BLOCK_CATEGORIES = "Categories";
    private static final String NAME_LOG = "Event Log";


    public MTransformControlsJPanel(MTransformJPanel mTransformJPanel){
        super(mTransformJPanel);
    }

    protected void generateGui(){

	// BLOCK LISTS ///////////
        JTabbedPane blockJTabbedPane = new JTabbedPane();
        blockJTabbedPane.setBorder(new EmptyBorder(7, 13, 13, 13));
        blockJTabbedPane.setFocusable(false);
        blockJTabbedPane.setFont(new java.awt.Font("Arial", 0, 11));
        blockJTabbedPane.setRequestFocusEnabled(false);
        this.mTabbedPane.addTab(NAME_BLOCK, null, blockJTabbedPane);

	// BLOCKED CATEGORIES /////////
	BlockedCategoriesConfigJPanel blockedCategoriesConfigJPanel = new BlockedCategoriesConfigJPanel();
	blockJTabbedPane.addTab(NAME_BLOCK_CATEGORIES, null, blockedCategoriesConfigJPanel);
	super.savableMap.put(NAME_BLOCK + " " + NAME_BLOCK_CATEGORIES, blockedCategoriesConfigJPanel);
	super.refreshableMap.put(NAME_BLOCK + " " + NAME_BLOCK_CATEGORIES, blockedCategoriesConfigJPanel);

	// BLOCKED URLS ///////////////
	BlockedURLsConfigJPanel blockedURLsConfigJPanel = new BlockedURLsConfigJPanel();
        blockJTabbedPane.addTab(NAME_BLOCK_URLS, null, blockedURLsConfigJPanel);
	super.savableMap.put(NAME_BLOCK + " " + NAME_BLOCK_URLS, blockedURLsConfigJPanel);
	super.refreshableMap.put(NAME_BLOCK + " " + NAME_BLOCK_URLS, blockedURLsConfigJPanel);

	// BLOCKED MIME TYPES ///////////
	BlockedMIMETypesConfigJPanel blockedMIMETypesConfigJPanel = new BlockedMIMETypesConfigJPanel();
        blockJTabbedPane.addTab(NAME_BLOCK_MIME_TYPES, null, blockedMIMETypesConfigJPanel);
	super.savableMap.put(NAME_BLOCK + " " + NAME_BLOCK_MIME_TYPES, blockedMIMETypesConfigJPanel);
	super.refreshableMap.put(NAME_BLOCK + " " + NAME_BLOCK_MIME_TYPES, blockedMIMETypesConfigJPanel);

	// BLOCKED FILE EXTENSIONS ///////
	BlockedExtensionsConfigJPanel blockedExtensionsConfigJPanel = new BlockedExtensionsConfigJPanel();
        blockJTabbedPane.addTab(NAME_BLOCK_FILE_EXTENSIONS, null, blockedExtensionsConfigJPanel);
	super.savableMap.put(NAME_BLOCK + " " + NAME_BLOCK_FILE_EXTENSIONS, blockedExtensionsConfigJPanel);
	super.refreshableMap.put(NAME_BLOCK + " " + NAME_BLOCK_FILE_EXTENSIONS, blockedExtensionsConfigJPanel);

	// PASS LISTS /////
        JTabbedPane passJTabbedPane = new JTabbedPane();
        passJTabbedPane.setBorder(new EmptyBorder(7, 13, 13, 13));
        passJTabbedPane.setFocusable(false);
        passJTabbedPane.setFont(new java.awt.Font("Arial", 0, 11));
        passJTabbedPane.setRequestFocusEnabled(false);
        this.mTabbedPane.addTab(NAME_PASS, null, passJTabbedPane);

	// PASSED URLS /////////
	PassedURLsConfigJPanel passedURLsConfigJPanel = new PassedURLsConfigJPanel();
        passJTabbedPane.addTab(NAME_PASS_URLS, null, passedURLsConfigJPanel);
	super.savableMap.put(NAME_PASS + " " + NAME_PASS_URLS, passedURLsConfigJPanel);
	super.refreshableMap.put(NAME_PASS + " " + NAME_PASS_URLS, passedURLsConfigJPanel);

	// PASSED CLIENTS ///////
	PassedClientsConfigJPanel passedClientsConfigJPanel = new PassedClientsConfigJPanel();
        passJTabbedPane.addTab(NAME_PASS_CLIENTS, null, passedClientsConfigJPanel);
	super.savableMap.put(NAME_PASS + " " + NAME_PASS_CLIENTS, passedClientsConfigJPanel);
	super.refreshableMap.put(NAME_PASS + " " + NAME_PASS_CLIENTS, passedClientsConfigJPanel);

	// EVENT LOG ///////
	LogJPanel logJPanel = new LogJPanel(mTransformJPanel.getTransformContext().transform());
        this.mTabbedPane.addTab(NAME_LOG, null, logJPanel);

	// SET TAB SELECTIONS /////////
        passJTabbedPane.setSelectedIndex(0);
        blockJTabbedPane.setSelectedIndex(0);

    }
    
}
