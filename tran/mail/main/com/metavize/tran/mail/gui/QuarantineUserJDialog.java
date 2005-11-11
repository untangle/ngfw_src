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
import com.metavize.tran.mail.papi.quarantine.*;

import java.lang.reflect.Constructor;

import java.awt.Dimension;
import java.awt.Dialog;
import java.util.*;
import javax.swing.table.*;
import javax.swing.*;



public class QuarantineUserJDialog extends MConfigJDialog {

    private static final String NAME_QUARANTINE_USER = "Email Quarantine Details for: ";
    private static final String NAME_ALL_ACCOUNTS = "Email Quarantine Details for: ";
    private QuarantineMaintenenceView quarantineMaintenenceView;
    private String account;
        
    public QuarantineUserJDialog(Dialog topLevelDialog, QuarantineMaintenenceView quarantineMaintenenceView, String account) {
	super(topLevelDialog, true);
        this.quarantineMaintenenceView = quarantineMaintenenceView;
        this.account = account;
	generateGuiAfter();
    }

    protected Dimension getMinSize(){
	return new Dimension(640, 550);
    }
    
    protected void generateGui(){}
    private void generateGuiAfter(){
        this.setTitle(NAME_QUARANTINE_USER + account);
        
        // ALL ACCOUNTS //////
        String casingName = "mail-casing";
        String objectName = "com.metavize.tran.mail.gui.QuarantineUserJPanel";
        JPanel quarantineAllJPanel = null;
        try{
            List<Tid> casingInstances = Util.getTransformManager().transformInstances(casingName);
            if( casingInstances.size() == 0 )
                return;
            TransformContext transformContext = Util.getTransformManager().transformContext(casingInstances.get(0));
            Class objectClass = Util.getClassLoader().loadClass( objectName, casingName );
            Constructor objectConstructor = objectClass.getConstructor(new Class[]{QuarantineMaintenenceView.class, String.class});
            quarantineAllJPanel = (JPanel) objectConstructor.newInstance(quarantineMaintenenceView, account);
        }
        catch(Exception e){
            Util.handleExceptionNoRestart("Error loading quarantine: " + casingName, e);
            return;
        }

        this.contentJTabbedPane.addTab(NAME_ALL_ACCOUNTS + account, null, quarantineAllJPanel);

        reloadJButton.setVisible(false);
        saveJButton.setVisible(false);
    }
    
    protected void sendSettings(Object settings) throws Exception { }
    protected void refreshSettings() {  }

}
