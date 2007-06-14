/*
 * $HeadURL:$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package com.untangle.node.virus.gui;

import javax.swing.JTabbedPane;

import com.untangle.gui.node.*;
import com.untangle.gui.util.*;
import com.untangle.uvm.*;
import com.untangle.uvm.node.*;

public class MNodeControlsJPanel extends com.untangle.gui.node.MNodeControlsJPanel{

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
    private static final String NAME_LOG = "Event Log";

    public MNodeControlsJPanel(MNodeJPanel mNodeJPanel) {
        super(mNodeJPanel);
    }

    public void generateGui(){

        // WEB ////////////////////
        JTabbedPane httpJTabbedPane = addTabbedPane(NAME_WEB, null);

        // WEB SOURCES ////////
        HTTPConfigJPanel httpConfigJPanel = new HTTPConfigJPanel();
        httpJTabbedPane.addTab(NAME_WEB_SOURCES, null, httpConfigJPanel);
        addSavable(NAME_WEB + " " + NAME_WEB_SOURCES, httpConfigJPanel);
        addRefreshable(NAME_WEB + " " + NAME_WEB_SOURCES, httpConfigJPanel);
        httpConfigJPanel.setSettingsChangedListener(this);

        // WEB EXTENSIONS ///////
        ExtensionsConfigJPanel extensionsConfigJPanel = new ExtensionsConfigJPanel();
        httpJTabbedPane.addTab(NAME_WEB_EXTENSIONS, null, extensionsConfigJPanel);
        addSavable(NAME_WEB + " " + NAME_WEB_EXTENSIONS, extensionsConfigJPanel);
        addRefreshable(NAME_WEB + " " + NAME_WEB_EXTENSIONS, extensionsConfigJPanel);
        extensionsConfigJPanel.setSettingsChangedListener(this);

        // WEB MIME ///////////
        MIMEConfigJPanel mimeConfigJPanel = new MIMEConfigJPanel();
        httpJTabbedPane.addTab(NAME_WEB_MIME, null, mimeConfigJPanel);
        addSavable(NAME_WEB + " " + NAME_WEB_MIME, mimeConfigJPanel);
        addRefreshable(NAME_WEB + " " + NAME_WEB_MIME, mimeConfigJPanel);
        mimeConfigJPanel.setSettingsChangedListener(this);

        // EMAIL //////////////////
        JTabbedPane emailJTabbedPane = addTabbedPane(NAME_EMAIL, null);

        // EMAIL SMTP /////////
        SmtpConfigJPanel smtpConfigJPanel = new SmtpConfigJPanel();
        emailJTabbedPane.addTab(NAME_EMAIL_SMTP, null, smtpConfigJPanel);
        addSavable(NAME_EMAIL + " " + NAME_EMAIL_SMTP, smtpConfigJPanel);
        addRefreshable(NAME_EMAIL + " " + NAME_EMAIL_SMTP, smtpConfigJPanel);
        smtpConfigJPanel.setSettingsChangedListener(this);

        // EMAIL POP /////////
        PopConfigJPanel popConfigJPanel = new PopConfigJPanel();
        emailJTabbedPane.addTab(NAME_EMAIL_POP, null, popConfigJPanel);
        addSavable(NAME_EMAIL + " " + NAME_EMAIL_POP, popConfigJPanel);
        addRefreshable(NAME_EMAIL + " " + NAME_EMAIL_POP, popConfigJPanel);
        popConfigJPanel.setSettingsChangedListener(this);

        // EMAIL IMAP /////////
        ImapConfigJPanel imapConfigJPanel = new ImapConfigJPanel();
        emailJTabbedPane.addTab(NAME_EMAIL_IMAP, null, imapConfigJPanel);
        addSavable(NAME_EMAIL + " " + NAME_EMAIL_IMAP, imapConfigJPanel);
        addRefreshable(NAME_EMAIL + " " + NAME_EMAIL_IMAP, imapConfigJPanel);
        imapConfigJPanel.setSettingsChangedListener(this);

        // FTP /////////////////////
        JTabbedPane ftpJTabbedPane = addTabbedPane(NAME_FTP, null);

        // FTP SOURCES /////////
        FTPConfigJPanel ftpConfigJPanel = new FTPConfigJPanel();
        ftpJTabbedPane.addTab(NAME_FTP_SOURCES, null, ftpConfigJPanel);
        addSavable(NAME_FTP + " " + NAME_FTP_SOURCES, ftpConfigJPanel);
        addRefreshable(NAME_FTP + " " + NAME_FTP_SOURCES, ftpConfigJPanel);
        ftpConfigJPanel.setSettingsChangedListener(this);

        // GENERAL CONFIG //////////
        GeneralConfigJPanel generalConfigJPanel = new GeneralConfigJPanel();
        addTab(NAME_GENERAL_SETTINGS, null, generalConfigJPanel);
        addSavable(NAME_GENERAL_SETTINGS, generalConfigJPanel);
        addRefreshable(NAME_GENERAL_SETTINGS, generalConfigJPanel);
        generalConfigJPanel.setSettingsChangedListener(this);

        // EVENT LOG ///////////////
        LogJPanel logJPanel = new LogJPanel(mNodeJPanel.getNode(), this);
        addTab(NAME_LOG, null, logJPanel);
        addShutdownable(NAME_LOG, logJPanel);
    }

}
