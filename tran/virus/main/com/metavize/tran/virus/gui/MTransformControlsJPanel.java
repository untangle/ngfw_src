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
    private static final String NAME_FTP = "File Transfer";
    private static final String NAME_FTP_SOURCES = "FTP";
    private static final String NAME_WEB = "Web";
    private static final String NAME_WEB_SOURCES = "HTTP";
    private static final String NAME_WEB_EXTENSIONS = "File Extension List";
    private static final String NAME_WEB_MIME = "MIME Type List";
    private static final String NAME_EMAIL = "Email";
    private static final String NAME_EMAIL_SMTP = "SMTP";
    private static final String NAME_EMAIL_POP = "POP";
    private static final String NAME_EMAIL_IMAP = "IMAP";

    public MTransformControlsJPanel(MTransformJPanel mTransformJPanel) {
        super(mTransformJPanel);
    }

    protected void generateGui(){
        
	// WEB ////////////////////
        JTabbedPane httpJTabbedPane = new JTabbedPane();
        httpJTabbedPane.setBorder(new EmptyBorder(7, 13, 13, 13));
        httpJTabbedPane.setFocusable(false);
        httpJTabbedPane.setFont(new java.awt.Font("Arial", 0, 11));
        httpJTabbedPane.setRequestFocusEnabled(false);
        this.mTabbedPane.addTab(NAME_WEB, null, httpJTabbedPane);
        
	// WEB SOURCES ////////
	HTTPConfigJPanel httpConfigJPanel = new HTTPConfigJPanel();
        httpJTabbedPane.addTab(NAME_WEB_SOURCES, null, httpConfigJPanel);
	this.savableMap.put(NAME_WEB + " " + NAME_WEB_SOURCES, httpConfigJPanel);
	this.refreshableMap.put(NAME_WEB + " " + NAME_WEB_SOURCES, httpConfigJPanel);
	
	// WEB MIME ///////////
	MIMEConfigJPanel mimeConfigJPanel = new MIMEConfigJPanel();
        httpJTabbedPane.addTab(NAME_WEB_MIME, null, mimeConfigJPanel);
	this.savableMap.put(NAME_WEB + " " + NAME_WEB_MIME, mimeConfigJPanel);
	this.refreshableMap.put(NAME_WEB + " " + NAME_WEB_MIME, mimeConfigJPanel);

	// WEB EXTENSIONS ///////
	ExtensionsConfigJPanel extensionsConfigJPanel = new ExtensionsConfigJPanel();
        httpJTabbedPane.addTab(NAME_WEB_EXTENSIONS, null, extensionsConfigJPanel);
	this.savableMap.put(NAME_WEB + " " + NAME_WEB_EXTENSIONS, extensionsConfigJPanel);
	this.refreshableMap.put(NAME_WEB + " " + NAME_WEB_EXTENSIONS, extensionsConfigJPanel);

	// EMAIL //////////////////
	JTabbedPane emailJTabbedPane = new JTabbedPane();
        emailJTabbedPane.setBorder(new EmptyBorder(7, 13, 13, 13));
        emailJTabbedPane.setFocusable(false);
        emailJTabbedPane.setFont(new java.awt.Font("Arial", 0, 11));
        emailJTabbedPane.setRequestFocusEnabled(false);
        this.mTabbedPane.addTab(NAME_EMAIL, null, emailJTabbedPane);

	// EMAIL SMTP /////////
	SmtpConfigJPanel smtpConfigJPanel = new SmtpConfigJPanel();
        emailJTabbedPane.addTab(NAME_EMAIL_SMTP, null, smtpConfigJPanel);
	this.savableMap.put(NAME_EMAIL + " " + NAME_EMAIL_SMTP, smtpConfigJPanel);
	this.refreshableMap.put(NAME_EMAIL + " " + NAME_EMAIL_SMTP, smtpConfigJPanel);

	// EMAIL POP /////////
	PopConfigJPanel popConfigJPanel = new PopConfigJPanel();
        emailJTabbedPane.addTab(NAME_EMAIL_POP, null, popConfigJPanel);
	this.savableMap.put(NAME_EMAIL + " " + NAME_EMAIL_POP, popConfigJPanel);
	this.refreshableMap.put(NAME_EMAIL + " " + NAME_EMAIL_POP, popConfigJPanel);

	// EMAIL IMAP /////////
	ImapConfigJPanel imapConfigJPanel = new ImapConfigJPanel();
        emailJTabbedPane.addTab(NAME_EMAIL_IMAP, null, imapConfigJPanel);
	this.savableMap.put(NAME_EMAIL + " " + NAME_EMAIL_IMAP, imapConfigJPanel);
	this.refreshableMap.put(NAME_EMAIL + " " + NAME_EMAIL_IMAP, imapConfigJPanel);

	// FTP /////////////////////
        JTabbedPane ftpJTabbedPane = new JTabbedPane();
        ftpJTabbedPane.setBorder(new EmptyBorder(7, 13, 13, 13));
        ftpJTabbedPane.setFocusable(false);
        ftpJTabbedPane.setFont(new java.awt.Font("Arial", 0, 11));
        ftpJTabbedPane.setRequestFocusEnabled(false);
        this.mTabbedPane.addTab(NAME_FTP, null, ftpJTabbedPane);

	// FTP SOURCES /////////
	FTPConfigJPanel ftpConfigJPanel = new FTPConfigJPanel();
        ftpJTabbedPane.addTab(NAME_FTP_SOURCES, null, ftpConfigJPanel);
	this.savableMap.put(NAME_FTP + " " + NAME_FTP_SOURCES, ftpConfigJPanel);
	this.refreshableMap.put(NAME_FTP + " " + NAME_FTP_SOURCES, ftpConfigJPanel);

        // GENERAL CONFIG //////////
	GeneralConfigJPanel generalConfigJPanel = new GeneralConfigJPanel();
        this.mTabbedPane.addTab(NAME_GENERAL_SETTINGS, null, generalConfigJPanel);
	this.savableMap.put(NAME_GENERAL_SETTINGS, generalConfigJPanel);
	this.refreshableMap.put(NAME_GENERAL_SETTINGS, generalConfigJPanel);

	// SET SELECTED TABS ///////
        httpJTabbedPane.setSelectedIndex(0);
    }
    
}
