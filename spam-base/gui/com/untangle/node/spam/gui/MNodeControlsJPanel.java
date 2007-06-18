/*
 * $HeadURL$
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



package com.untangle.node.spam.gui;

import com.untangle.gui.node.*;
import com.untangle.gui.util.*;


public class MNodeControlsJPanel extends com.untangle.gui.node.MNodeControlsJPanel{

    private static final String NAME_SPAM_SMTP = "SMTP";
    private static final String NAME_SPAM_POP = "POP";
    private static final String NAME_SPAM_IMAP = "IMAP";
    private static final String NAME_LOG = "Event Log";
    private static final String NAME_RBL_LOG = "DNSBL Event Log";

    public MNodeControlsJPanel(MNodeJPanel mNodeJPanel)  {
        super(mNodeJPanel);
    }

    public void generateGui(){
        // SMTP ////////
        SmtpConfigJPanel smtpConfigJPanel = new SmtpConfigJPanel();
        addTab(NAME_SPAM_SMTP, null, smtpConfigJPanel);
        addSavable(NAME_SPAM_SMTP, smtpConfigJPanel);
        addRefreshable(NAME_SPAM_SMTP, smtpConfigJPanel);
        smtpConfigJPanel.setSettingsChangedListener(this);

        // POP ////////
        PopConfigJPanel popConfigJPanel = new PopConfigJPanel();
        addTab(NAME_SPAM_POP, null, popConfigJPanel);
        addSavable(NAME_SPAM_POP, popConfigJPanel);
        addRefreshable(NAME_SPAM_POP, popConfigJPanel);
        popConfigJPanel.setSettingsChangedListener(this);

        // IMAP ////////
        ImapConfigJPanel imapConfigJPanel = new ImapConfigJPanel();
        addTab(NAME_SPAM_IMAP, null, imapConfigJPanel);
        addSavable(NAME_SPAM_IMAP, imapConfigJPanel);
        addRefreshable(NAME_SPAM_IMAP, imapConfigJPanel);
        imapConfigJPanel.setSettingsChangedListener(this);

        // EVENT LOG /////
        LogJPanel logJPanel = new LogJPanel(mNodeJPanel.getNode(), this);
        addTab(NAME_LOG, null, logJPanel);
        addShutdownable(NAME_LOG, logJPanel);

        // RBL EVENT LOG /////
        LogRblJPanel logRblJPanel = new LogRblJPanel(mNodeJPanel.getNode(), this);
        addTab(NAME_RBL_LOG, null, logRblJPanel);
        addShutdownable(NAME_RBL_LOG, logRblJPanel);
    }

}

