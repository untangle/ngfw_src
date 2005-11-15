/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.mail.gui;

import com.metavize.gui.widgets.dialogs.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;

import com.metavize.mvvm.tran.*;
import com.metavize.mvvm.security.Tid;

import com.metavize.tran.mail.papi.*;
import com.metavize.tran.mail.papi.safelist.*;

import java.lang.reflect.Constructor;

import java.awt.Dimension;
import java.awt.Dialog;
import java.util.*;
import javax.swing.table.*;
import javax.swing.*;



public class WhitelistUserJDialog extends MConfigJDialog {

    private static final String NAME_WHITELIST_USER = "Email Quarantine Whitelist Details for: ";
    private static final String NAME_ALL_ACCOUNTS = "Email Quarantine Whitelist Details for: ";
    private SafelistAdminView safelistAdminView;
    private String account;
        
    public WhitelistUserJDialog(Dialog topLevelDialog, SafelistAdminView safelistAdminView, String account) {
	super(topLevelDialog, true);
        this.safelistAdminView = safelistAdminView;
        this.account = account;
	generateGuiAfter();
    }

    protected Dimension getMinSize(){
	return new Dimension(640, 550);
    }
    
    protected void generateGui(){}
    private void generateGuiAfter(){
        this.setTitle(NAME_WHITELIST_USER + account);
        
        // ALL ACCOUNTS //////
        String casingName = "mail-casing";
        String objectName = "com.metavize.tran.mail.gui.WhitelistUserJPanel";
        JPanel whitelistAllJPanel = null;
        try{
            Class objectClass = Util.getClassLoader().loadClass( objectName, casingName );
            Constructor objectConstructor = objectClass.getConstructor(new Class[]{SafelistAdminView.class, String.class});
            whitelistAllJPanel = (JPanel) objectConstructor.newInstance(safelistAdminView, account);
        }
        catch(Exception e){
            Util.handleExceptionNoRestart("Error loading whitelist: " + casingName, e);
            return;
        }

        this.contentJTabbedPane.addTab(NAME_ALL_ACCOUNTS + account, null, whitelistAllJPanel);

        reloadJButton.setVisible(false);
        saveJButton.setVisible(false);
    }
    
    protected void sendSettings(Object settings) throws Exception { }
    protected void refreshSettings() {  }

}
