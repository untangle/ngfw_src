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



package com.metavize.tran.clamphish.gui;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.gui.util.*;

import com.metavize.mvvm.tran.TransformContext;

public class MTransformControlsJPanel extends com.metavize.gui.transform.MTransformControlsJPanel{
    
    private static final String NAME_SPAM_SMTP = "SMTP";
    private static final String NAME_SPAM_POP = "POP";
    private static final String NAME_SPAM_IMAP = "IMAP";
    private static final String NAME_LOG = "Event Log";
    
    public MTransformControlsJPanel(MTransformJPanel mTransformJPanel)  {
        super(mTransformJPanel);
    }

    protected void generateGui(){
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
	LogJPanel logJPanel = new LogJPanel(mTransformJPanel.getTransformContext().transform(), this);
	addTab(NAME_LOG, null, logJPanel);
	addShutdownable(NAME_LOG, logJPanel);
    }
    
}

