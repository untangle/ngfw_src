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

package com.metavize.gui.configuration;

import com.metavize.gui.widgets.dialogs.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;

import com.metavize.mvvm.tran.*;
import com.metavize.mvvm.security.Tid;

import java.lang.reflect.Constructor;

import java.awt.Dimension;
import java.util.*;
import javax.swing.table.*;
import javax.swing.*;



public class QuarantineJDialog extends MConfigJDialog {

    private static final String NAME_QUARANTINE_SETTINGS = "Quarantine Settings";
    private static final String NAME_ALL_ACCOUNTS = "All Email Accounts";

    public QuarantineJDialog( ) {
    }

    protected Dimension getMinSize(){
	return new Dimension(640, 550);
    }
    
    protected void generateGui(){
        this.setTitle(NAME_QUARANTINE_SETTINGS);
        
        // ALL ACCOUNTS //////
        String casingName = "mail-casing";
        String objectName = "com.metavize.tran.mail.gui.QuarantineAllJPanel";
        JPanel quarantineAllJPanel = null;
        try{
            List<Tid> casingInstances = Util.getTransformManager().transformInstances(casingName);
            if( casingInstances.size() == 0 )
                return;
            TransformContext transformContext = Util.getTransformManager().transformContext(casingInstances.get(0));
            Class objectClass = Util.getClassLoader().loadClass( objectName, casingName );
            Constructor objectConstructor = objectClass.getConstructor(new Class[]{});
            quarantineAllJPanel = (JPanel) objectConstructor.newInstance();
        }
        catch(Exception e){
            Util.handleExceptionNoRestart("Error loading quarantine: " + casingName, e);
            return;
        }

        this.contentJTabbedPane.addTab(NAME_ALL_ACCOUNTS, null, quarantineAllJPanel);

        reloadJButton.setVisible(false);
        saveJButton.setVisible(false);
    }
    
    protected void sendSettings(Object settings) throws Exception { }
    protected void refreshSettings() {  }

}
