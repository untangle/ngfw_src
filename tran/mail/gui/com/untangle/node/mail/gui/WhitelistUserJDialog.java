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

package com.untangle.node.mail.gui;

import java.awt.Dialog;
import java.awt.Dimension;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;

import com.untangle.gui.util.*;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.uvm.node.*;
import com.untangle.node.mail.papi.*;
import com.untangle.node.mail.papi.safelist.*;



public class WhitelistUserJDialog extends MConfigJDialog {

    private static final String NAME_WHITELIST_USER = "Email From-Safe List Details for: ";

    private SafelistAdminView safelistAdminView;
    private String account;

    public WhitelistUserJDialog(Dialog topLevelDialog, MailNodeCompoundSettings mailNodeCompoundSettings, String account) {
        super(topLevelDialog);
        setHelpSource("email_config");
        compoundSettings = mailNodeCompoundSettings;
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
        ((MailNodeCompoundSettings)compoundSettings).loadSafelistContents(account);
    }
}
