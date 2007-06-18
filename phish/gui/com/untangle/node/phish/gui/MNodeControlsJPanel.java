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



package com.untangle.node.phish.gui;

import com.untangle.gui.node.*;
import com.untangle.gui.util.*;


public class MNodeControlsJPanel extends com.untangle.gui.node.MNodeControlsJPanel{

    private static final String NAME_SPAM_GOOGLE = "Web";
    private static final String NAME_WEB_LOG = "Web Event Log";

    private static final String NAME_SPAM_SMTP = "SMTP";
    private static final String NAME_SPAM_POP = "POP";
    private static final String NAME_SPAM_IMAP = "IMAP";
    private static final String NAME_EMAIL_LOG = "Email Event Log";

    public MNodeControlsJPanel(MNodeJPanel mNodeJPanel)  {
        super(mNodeJPanel);
    }

    public void generateGui(){
        // HTTP ////////
        GoogleConfigJPanel googleConfigJPanel = new GoogleConfigJPanel();
        addTab(NAME_SPAM_GOOGLE, null, googleConfigJPanel);
        addSavable(NAME_SPAM_GOOGLE, googleConfigJPanel);
        addRefreshable(NAME_SPAM_GOOGLE, googleConfigJPanel);
        googleConfigJPanel.setSettingsChangedListener(this);

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

        // WEB EVENT LOG /////
        WebLogJPanel webLogJPanel = new WebLogJPanel(mNodeJPanel.getNode(), this);
        addTab(NAME_WEB_LOG, null, webLogJPanel);
        addShutdownable(NAME_WEB_LOG, webLogJPanel);

        // EMAIL EVENT LOG /////
        EmailLogJPanel emailLogJPanel = new EmailLogJPanel(mNodeJPanel.getNode(), this);
        addTab(NAME_EMAIL_LOG, null, emailLogJPanel);
        addShutdownable(NAME_EMAIL_LOG, emailLogJPanel);
    }

}

