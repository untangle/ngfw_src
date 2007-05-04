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



package com.untangle.tran.spam.gui;

import com.untangle.gui.transform.*;
import com.untangle.gui.util.*;


public class MTransformControlsJPanel extends com.untangle.gui.transform.MTransformControlsJPanel{

    private static final String NAME_SPAM_SMTP = "SMTP";
    private static final String NAME_SPAM_POP = "POP";
    private static final String NAME_SPAM_IMAP = "IMAP";
    private static final String NAME_LOG = "Event Log";
    private static final String NAME_RBL_LOG = "DNSBL Event Log";

    public MTransformControlsJPanel(MTransformJPanel mTransformJPanel)  {
        super(mTransformJPanel);
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
        LogJPanel logJPanel = new LogJPanel(mTransformJPanel.getTransform(), this);
        addTab(NAME_LOG, null, logJPanel);
        addShutdownable(NAME_LOG, logJPanel);

        // RBL EVENT LOG /////
        LogRblJPanel logRblJPanel = new LogRblJPanel(mTransformJPanel.getTransform(), this);
        addTab(NAME_RBL_LOG, null, logRblJPanel);
        addShutdownable(NAME_RBL_LOG, logRblJPanel);
    }

}

