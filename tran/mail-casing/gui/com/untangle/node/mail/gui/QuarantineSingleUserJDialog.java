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
import com.untangle.node.mail.papi.quarantine.*;



public class QuarantineSingleUserJDialog extends MConfigJDialog {

    private static final String NAME_ALL_ACCOUNTS    = "Email Quarantine Details for: ";

    private String account;

    public QuarantineSingleUserJDialog(Dialog topLevelDialog, MailNodeCompoundSettings mailNodeCompoundSettings, String account) {
        super(topLevelDialog);
        setHelpSource("email_config");
        compoundSettings = mailNodeCompoundSettings;
        this.account = account;
        INSTANCE = this;
    }

    protected Dimension getMinSize(){
        return new Dimension(640, 550);
    }

    private static QuarantineSingleUserJDialog INSTANCE;
    public static QuarantineSingleUserJDialog instance(){ return INSTANCE; }

    protected void generateGui(){
        this.setTitle(NAME_ALL_ACCOUNTS + account);
        saveJButton.setVisible(false);

        // ALL ACCOUNTS //////
        QuarantineSingleUserJPanel quarantineSingleUserJPanel = new QuarantineSingleUserJPanel(account);
        addRefreshable(NAME_ALL_ACCOUNTS, quarantineSingleUserJPanel);
        addTab(NAME_ALL_ACCOUNTS + account, null, quarantineSingleUserJPanel);
    }

    protected void refreshAll() throws Exception {
        super.refreshAll();
        ((MailNodeCompoundSettings)compoundSettings).loadInboxIndex(account);
    }
}
