/*
 * 
 *
 * Created on March 25, 2004, 6:11 PM
 */

package com.metavize.tran.spam.gui;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.gui.util.*;

import com.metavize.mvvm.tran.TransformContext;

public class MTransformControlsJPanel extends com.metavize.gui.transform.MTransformControlsJPanel{
    
    private static final String NAME_SPAM_SMTP = "SMTP";
    private static final String NAME_SPAM_POP = "POP";
    private static final String NAME_SPAM_IMAP = "IMAP";
    
    public MTransformControlsJPanel(MTransformJPanel mTransformJPanel)  {
        super(mTransformJPanel);
    }

    protected void generateGui(){

	// SMTP ////////
	SmtpConfigJPanel smtpConfigJPanel = new SmtpConfigJPanel();
	this.mTabbedPane.addTab(NAME_SPAM_SMTP, null, smtpConfigJPanel);
	super.savableMap.put(NAME_SPAM_SMTP, smtpConfigJPanel);
	super.refreshableMap.put(NAME_SPAM_SMTP, smtpConfigJPanel);

	// POP ////////
	PopConfigJPanel popConfigJPanel = new PopConfigJPanel();
	this.mTabbedPane.addTab(NAME_SPAM_POP, null, popConfigJPanel);
	super.savableMap.put(NAME_SPAM_POP, popConfigJPanel);
	super.refreshableMap.put(NAME_SPAM_POP, popConfigJPanel);

	// IMAP ////////
	ImapConfigJPanel imapConfigJPanel = new ImapConfigJPanel();
	this.mTabbedPane.addTab(NAME_SPAM_IMAP, null, imapConfigJPanel);
	super.savableMap.put(NAME_SPAM_IMAP, imapConfigJPanel);
	super.refreshableMap.put(NAME_SPAM_IMAP, imapConfigJPanel);

    }
    
}

