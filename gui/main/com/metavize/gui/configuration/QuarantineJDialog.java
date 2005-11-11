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

import com.metavize.gui.transform.*;
import com.metavize.mvvm.tran.*;
import com.metavize.mvvm.security.Tid;

import java.lang.reflect.Constructor;

import java.awt.Dimension;
import java.util.*;
import javax.swing.table.*;
import javax.swing.*;



public class QuarantineJDialog extends MConfigJDialog {

    private static final String NAME_QUARANTINE_SETTINGS = "Email Quarantine";
    private static final String NAME_ALL_ACCOUNTS = "All Email Accounts";
    private static final String NAME_GENERAL_SETTINGS = "General Settings";

    public QuarantineJDialog( ) {
    }

    protected Dimension getMinSize(){
	return new Dimension(640, 550);
    }
    
    protected void generateGui(){
        this.setTitle(NAME_QUARANTINE_SETTINGS);
        
	// GET TRANSFORM CONTEXT //
        String casingName = "mail-casing";
	TransformContext transformContext;
	try{
	    List<Tid> casingInstances = Util.getTransformManager().transformInstances(casingName);
	    if( casingInstances.size() == 0 )
		return;
	    transformContext = Util.getTransformManager().transformContext(casingInstances.get(0));
	}
	catch(Exception e){
            Util.handleExceptionNoRestart("Error loading mail casing: " + casingName, e);
            return;
	}
	
        // ALL ACCOUNTS //////
        String quarantineAllJPanelName = "com.metavize.tran.mail.gui.QuarantineAllJPanel";
        JPanel quarantineAllJPanel = null;
        try{
            Class objectClass = Util.getClassLoader().loadClass( quarantineAllJPanelName, casingName );
            Constructor objectConstructor = objectClass.getConstructor(new Class[]{TransformContext.class});
            quarantineAllJPanel = (JPanel) objectConstructor.newInstance(transformContext);
        }
        catch(Exception e){
            Util.handleExceptionNoRestart("Error loading quarantine management: " + casingName, e);
            return;
        }
        super.contentJTabbedPane.addTab(NAME_ALL_ACCOUNTS, null, quarantineAllJPanel);

        // GENERAL SETTINGS //////
        String quarantineGeneralSettingsJPanelName = "com.metavize.tran.mail.gui.QuarantineGeneralSettingsJPanel";
        JPanel quarantineGeneralSettingsJPanel = null;
        try{
            Class objectClass = Util.getClassLoader().loadClass( quarantineGeneralSettingsJPanelName, casingName );
            Constructor objectConstructor = objectClass.getConstructor(new Class[]{TransformContext.class});
            quarantineGeneralSettingsJPanel = (JPanel) objectConstructor.newInstance(transformContext);
        }
        catch(Exception e){
            Util.handleExceptionNoRestart("Error loading quarantine general settings: " + casingName, e);
            return;
        }
        super.contentJTabbedPane.addTab(NAME_GENERAL_SETTINGS, null, quarantineGeneralSettingsJPanel);
	super.savableMap.put(NAME_GENERAL_SETTINGS, (Savable) quarantineGeneralSettingsJPanel);
	super.refreshableMap.put(NAME_GENERAL_SETTINGS, (Refreshable) quarantineGeneralSettingsJPanel);
    }
    
    protected void sendSettings(Object settings) throws Exception { }
    protected void refreshSettings() {  }

}
