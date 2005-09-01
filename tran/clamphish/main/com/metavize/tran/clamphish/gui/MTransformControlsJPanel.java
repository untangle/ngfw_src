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
	super.mTabbedPane.addTab(NAME_SPAM_SMTP, null, smtpConfigJPanel);
	super.savableMap.put(NAME_SPAM_SMTP, smtpConfigJPanel);
	super.refreshableMap.put(NAME_SPAM_SMTP, smtpConfigJPanel);

	// POP ////////
	PopConfigJPanel popConfigJPanel = new PopConfigJPanel();
	super.mTabbedPane.addTab(NAME_SPAM_POP, null, popConfigJPanel);
	super.savableMap.put(NAME_SPAM_POP, popConfigJPanel);
	super.refreshableMap.put(NAME_SPAM_POP, popConfigJPanel);

	// IMAP ////////
	ImapConfigJPanel imapConfigJPanel = new ImapConfigJPanel();
	super.mTabbedPane.addTab(NAME_SPAM_IMAP, null, imapConfigJPanel);
	super.savableMap.put(NAME_SPAM_IMAP, imapConfigJPanel);
	super.refreshableMap.put(NAME_SPAM_IMAP, imapConfigJPanel);

	// EVENT LOG /////
	LogJPanel logJPanel = new LogJPanel(mTransformJPanel.getTransformContext().transform(), this);
	super.mTabbedPane.addTab(NAME_LOG, null, logJPanel);
	super.shutdownableMap.put(NAME_LOG, logJPanel);
    }
    
}

