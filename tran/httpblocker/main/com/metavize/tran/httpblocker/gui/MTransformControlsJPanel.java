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


    public MTransformControlsJPanel(MTransformJPanel mTransformJPanel){
        super(mTransformJPanel);
    }

    protected void generateGui(){

	// PASS LISTS /////
        JTabbedPane passJTabbedPane = new JTabbedPane();
        passJTabbedPane.setBorder(new EmptyBorder(7, 13, 13, 13));
        passJTabbedPane.setFocusable(false);
        passJTabbedPane.setFont(new java.awt.Font("Arial", 0, 11));
        passJTabbedPane.setRequestFocusEnabled(false);
        this.mTabbedPane.insertTab(NAME_PASS, null, passJTabbedPane, null, 0);

	// PASSED CLIENTS ///////
	PassedClientsConfigJPanel passedClientsConfigJPanel = new PassedClientsConfigJPanel();
        passJTabbedPane.insertTab(NAME_PASS_CLIENTS, null, passedClientsConfigJPanel, null, 0);
	super.savableMap.put(NAME_PASS + " " + NAME_PASS_CLIENTS, passedClientsConfigJPanel);
	super.refreshableMap.put(NAME_PASS + " " + NAME_PASS_CLIENTS, passedClientsConfigJPanel);

	// PASSED URLS /////////
	PassedURLsConfigJPanel passedURLsConfigJPanel = new PassedURLsConfigJPanel();
        passJTabbedPane.insertTab(NAME_PASS_URLS, null, passedURLsConfigJPanel, null, 0);
	super.savableMap.put(NAME_PASS + " " + NAME_PASS_URLS, passedURLsConfigJPanel);  // a is added to make this key unique from the other URLs key
	super.refreshableMap.put(NAME_PASS + " " + NAME_PASS_URLS, passedURLsConfigJPanel);

	// BLOCK LISTS ///////////
        JTabbedPane blockJTabbedPane = new JTabbedPane();
        blockJTabbedPane.setBorder(new EmptyBorder(7, 13, 13, 13));
        blockJTabbedPane.setFocusable(false);
        blockJTabbedPane.setFont(new java.awt.Font("Arial", 0, 11));
        blockJTabbedPane.setRequestFocusEnabled(false);
        this.mTabbedPane.insertTab(NAME_BLOCK, null, blockJTabbedPane, null, 0);        
        
	// BLOCKED FILE EXTENSIONS ///////
	BlockedExtensionsConfigJPanel blockedExtensionsConfigJPanel = new BlockedExtensionsConfigJPanel();
        blockJTabbedPane.insertTab(NAME_BLOCK_FILE_EXTENSIONS, null, blockedExtensionsConfigJPanel, null, 0);
	super.savableMap.put(NAME_BLOCK + " " + NAME_BLOCK_FILE_EXTENSIONS, blockedExtensionsConfigJPanel);
	super.refreshableMap.put(NAME_BLOCK + " " + NAME_BLOCK_FILE_EXTENSIONS, blockedExtensionsConfigJPanel);

	// BLOCKED MIME TYPES ///////////
	BlockedMIMETypesConfigJPanel blockedMIMETypesConfigJPanel = new BlockedMIMETypesConfigJPanel();
        blockJTabbedPane.insertTab(NAME_BLOCK_MIME_TYPES, null, blockedMIMETypesConfigJPanel, null, 0);
	super.savableMap.put(NAME_BLOCK + " " + NAME_BLOCK_MIME_TYPES, blockedMIMETypesConfigJPanel);
	super.refreshableMap.put(NAME_BLOCK + " " + NAME_BLOCK_MIME_TYPES, blockedMIMETypesConfigJPanel);

	// BLOCKED URLS ///////////////
	BlockedURLsConfigJPanel blockedURLsConfigJPanel = new BlockedURLsConfigJPanel();
        blockJTabbedPane.insertTab(NAME_BLOCK_URLS, null, blockedURLsConfigJPanel, null, 0);
	super.savableMap.put(NAME_BLOCK + " " + NAME_BLOCK_URLS, blockedURLsConfigJPanel);
	super.refreshableMap.put(NAME_BLOCK + " " + NAME_BLOCK_URLS, blockedURLsConfigJPanel);

	// BLOCKED CATEGORIES /////////
	BlockedCategoriesConfigJPanel blockedCategoriesConfigJPanel = new BlockedCategoriesConfigJPanel();
	blockJTabbedPane.insertTab(NAME_BLOCK_CATEGORIES, null, blockedCategoriesConfigJPanel, null, 0);
	super.savableMap.put(NAME_BLOCK + " " + NAME_BLOCK_CATEGORIES, blockedCategoriesConfigJPanel);
	super.refreshableMap.put(NAME_BLOCK + " " + NAME_BLOCK_CATEGORIES, blockedCategoriesConfigJPanel);

	// SET TAB SELECTIONS /////////
        passJTabbedPane.setSelectedIndex(0);
        blockJTabbedPane.setSelectedIndex(0);

    }
    
}
