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

package com.untangle.tran.mail.gui;

import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.gui.util.*;

import com.untangle.mvvm.tran.*;
import com.untangle.mvvm.security.Tid;

import com.untangle.tran.mail.papi.*;
import com.untangle.tran.mail.papi.safelist.*;

import java.lang.reflect.Constructor;

import java.awt.Dimension;
import java.awt.Dialog;
import java.util.*;
import javax.swing.table.*;
import javax.swing.*;



public class WhitelistUserJDialog extends MConfigJDialog {

    private static final String NAME_WHITELIST_USER = "Email From-Safe List Details for: ";

    private SafelistAdminView safelistAdminView;
    private String account;
        
    public WhitelistUserJDialog(Dialog topLevelDialog, MailTransformCompoundSettings mailTransformCompoundSettings, String account) {
	super(topLevelDialog);
	compoundSettings = mailTransformCompoundSettings;
        this.account = account;
	INSTANCE = this;
    }

    protected Dimension getMinSize(){
	return new Dimension(640, 550);
    }
    
    private static WhitelistUserJDialog INSTANCE;
    public static WhitelistUserJDialog instance(){ return INSTANCE; }
    
    protected void generateGui(){
        this.setTitle(NAME_WHITELIST_USER + account);
        saveJButton.setVisible(false);
        
        // ALL ACCOUNTS //////
	WhitelistUserJPanel whitelistUserJPanel = new WhitelistUserJPanel(account);
	addRefreshable(NAME_WHITELIST_USER, whitelistUserJPanel);
        addTab(NAME_WHITELIST_USER + account, null, whitelistUserJPanel);
    }
    
    protected void refreshAll() throws Exception {
	super.refreshAll();
	((MailTransformCompoundSettings)compoundSettings).loadSafelistContents(account);
    }
}
