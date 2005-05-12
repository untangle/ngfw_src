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
package com.metavize.tran.virus.gui;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.gui.util.*;

import com.metavize.mvvm.*;
import com.metavize.mvvm.tran.*;


import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;

public class MTransformControlsJPanel extends com.metavize.gui.transform.MTransformControlsJPanel{
    
    private static final String NAME_GENERAL_SETTINGS = "General Settings";
    private static final String NAME_FTP = "FTP";
    private static final String NAME_FTP_SOURCES = "Sources";
    private static final String NAME_WEB = "Web";
    private static final String NAME_WEB_SOURCES = "Sources";
    private static final String NAME_WEB_EXTENSIONS = "File Extension List (from Sources)";
    private static final String NAME_WEB_MIME = "MIME Type List (from Sources)";

    public MTransformControlsJPanel(MTransformJPanel mTransformJPanel) {
        super(mTransformJPanel);
    }

    protected void generateGui(){
        
	// EMAIL DETECTION ////////
        EmailDetectionJPanel emailDetectionJPanel = new EmailDetectionJPanel();
        String transformName = mTransformJPanel.getTransformContext().getTransformDesc().getName();
	Util.setEmailAndVirusJPanel( transformName, emailDetectionJPanel );

        // GENERAL CONFIG //////////
	GeneralConfigJPanel generalConfigJPanel = new GeneralConfigJPanel();
        this.mTabbedPane.insertTab(NAME_GENERAL_SETTINGS, null, generalConfigJPanel, null, 0);
	this.savableMap.put(NAME_GENERAL_SETTINGS, generalConfigJPanel);
	this.refreshableMap.put(NAME_GENERAL_SETTINGS, generalConfigJPanel);

	// EMAIL //////////////////
        this.mTabbedPane.insertTab("eMail", null, emailDetectionJPanel, null, 0);

	// FTP /////////////////////
        JTabbedPane ftpJTabbedPane = new JTabbedPane();
        ftpJTabbedPane.setBorder(new EmptyBorder(7, 13, 13, 13));
        ftpJTabbedPane.setFocusable(false);
        ftpJTabbedPane.setFont(new java.awt.Font("Arial", 0, 11));
        ftpJTabbedPane.setRequestFocusEnabled(false);
        this.mTabbedPane.insertTab(NAME_FTP, null, ftpJTabbedPane, null, 0);

	// FTP SOURCES /////////
	FTPConfigJPanel ftpConfigJPanel = new FTPConfigJPanel();
        ftpJTabbedPane.insertTab(NAME_FTP_SOURCES, null, ftpConfigJPanel, null, 0);
	this.savableMap.put(NAME_FTP + " " + NAME_FTP_SOURCES, ftpConfigJPanel);
	this.refreshableMap.put(NAME_FTP + " " + NAME_FTP_SOURCES, ftpConfigJPanel);

	// WEB ////////////////////
        JTabbedPane httpJTabbedPane = new JTabbedPane();
        httpJTabbedPane.setBorder(new EmptyBorder(7, 13, 13, 13));
        httpJTabbedPane.setFocusable(false);
        httpJTabbedPane.setFont(new java.awt.Font("Arial", 0, 11));
        httpJTabbedPane.setRequestFocusEnabled(false);
        this.mTabbedPane.insertTab(NAME_WEB, null, httpJTabbedPane, null, 0);
        
	// WEB EXTENSIONS ///////
	ExtensionsConfigJPanel extensionsConfigJPanel = new ExtensionsConfigJPanel();
        httpJTabbedPane.insertTab(NAME_WEB_EXTENSIONS, null, extensionsConfigJPanel, null, 0);
	this.savableMap.put(NAME_WEB + " " + NAME_WEB_EXTENSIONS, extensionsConfigJPanel);
	this.refreshableMap.put(NAME_WEB + " " + NAME_WEB_EXTENSIONS, extensionsConfigJPanel);
	
	// WEB MIME ///////////
	MIMEConfigJPanel mimeConfigJPanel = new MIMEConfigJPanel();
        httpJTabbedPane.insertTab(NAME_WEB_MIME, null, mimeConfigJPanel, null, 0);
	this.savableMap.put(NAME_WEB + " " + NAME_WEB_MIME, mimeConfigJPanel);
	this.refreshableMap.put(NAME_WEB + " " + NAME_WEB_MIME, mimeConfigJPanel);

	// WEB SOURCES ////////
	HTTPConfigJPanel httpConfigJPanel = new HTTPConfigJPanel();
        httpJTabbedPane.insertTab(NAME_WEB_SOURCES, null, httpConfigJPanel, null, 0);
	this.savableMap.put(NAME_WEB + " " + NAME_WEB_SOURCES, httpConfigJPanel);
	this.refreshableMap.put(NAME_WEB + " " + NAME_WEB_SOURCES, httpConfigJPanel);

	// SET SELECTED TABS ///////
        httpJTabbedPane.setSelectedIndex(0);
    }
    
}
