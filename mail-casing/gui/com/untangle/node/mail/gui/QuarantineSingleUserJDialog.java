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
