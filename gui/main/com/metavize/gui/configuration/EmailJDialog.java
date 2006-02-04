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
import java.awt.BorderLayout;
import java.util.*;
import javax.swing.table.*;
import javax.swing.*;


public class EmailJDialog extends MConfigJDialog {

    private static final String NAME_EMAIL_CONFIG = "Email Config";
    private static final String NAME_OUTGOING_SETTINGS = "Outgoing Server";
    private static final String NAME_SAFE_LIST = "From-Safe List";
    private static final String NAME_QUARANTINE_SETTINGS = "Quarantine";
    private static final String NAME_ALL_ACCOUNTS = "Release & Purge";
    private static final String NAME_QUARANTINABLE_ADDRESSES = "Quarantinable Addresses";
    private static final String NAME_QUARANTINABLE_FORWARDS = "Quarantinable Forwards";
    private static final String NAME_GENERAL_SETTINGS = "General Settings";

    public EmailJDialog( ) {
    }

    protected Dimension getMinSize(){
	return new Dimension(640, 550);
    }
    
    protected void generateGui(){
        this.setTitle(NAME_EMAIL_CONFIG);

        // OUTGOING SERVER /////
        EmailOutgoingJPanel emailOutgoingJPanel = new EmailOutgoingJPanel();
	addScrollableTab(null, NAME_OUTGOING_SETTINGS, null, emailOutgoingJPanel, false, true);
	addSavable(NAME_OUTGOING_SETTINGS, emailOutgoingJPanel );
	addRefreshable(NAME_OUTGOING_SETTINGS, emailOutgoingJPanel );
        
	// GET TRANSFORM CONTEXT //
        String casingName = "mail-casing";
	TransformContext transformContext = null;
	TransformDesc transformDesc = null;
	try{
	    List<Tid> casingInstances = Util.getTransformManager().transformInstances(casingName);
	    if( casingInstances.size() == 0 ){
		/*
		JPanel messageJPanel = new JPanel();
		messageJPanel.setLayout(new BorderLayout());
		JLabel messageJLabel = new JLabel("<html>There are currently no Software Appliances in<br>"
						  + "the rack that need a Safe List.</html>");
		messageJLabel.setHorizontalAlignment(SwingConstants.CENTER);
		messageJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
		messageJPanel.add(messageJLabel);
		addTab(NAME_SAFE_LIST, null, messageJPanel);
		*/
		return;
	    }
	    transformContext = Util.getTransformManager().transformContext(casingInstances.get(0));
	    transformDesc = transformContext.getTransformDesc();
	}
	catch(Exception e){
	    try{
		Util.handleExceptionWithRestart("Error loading mail casing: ", e);
	    }
	    catch(Exception f){
		Util.handleExceptionNoRestart("Error loading mail casing: " + casingName, f);
		return;
	    }
	}

        // SAFELIST CONTROLS //////
        String whitelistAllJPanelName = "com.metavize.tran.mail.gui.WhitelistAllJPanel";
        JPanel whitelistAllJPanel = null;
        try{
            Class objectClass = Util.getClassLoader().loadClass( whitelistAllJPanelName, transformDesc );
            Constructor objectConstructor = objectClass.getConstructor(new Class[]{TransformContext.class});
            whitelistAllJPanel = (JPanel) objectConstructor.newInstance(transformContext);
        }
        catch(Exception e){
            Util.handleExceptionNoRestart("Error loading whitelist management: " + casingName, e);
            return;
        }
        addTab(NAME_SAFE_LIST, null, whitelistAllJPanel);
	
	// QUARANTINE ///////
	JTabbedPane quarantineJTabbedPane = addTabbedPane(NAME_QUARANTINE_SETTINGS, null);

        // QUARANTINE RELEASE & PURGE //////
        String quarantineAllJPanelName = "com.metavize.tran.mail.gui.QuarantineAllJPanel";
        JPanel quarantineAllJPanel = null;
        try{
            Class objectClass = Util.getClassLoader().loadClass( quarantineAllJPanelName, transformDesc );
            Constructor objectConstructor = objectClass.getConstructor(new Class[]{TransformContext.class});
            quarantineAllJPanel = (JPanel) objectConstructor.newInstance(transformContext);
        }
        catch(Exception e){
            Util.handleExceptionNoRestart("Error loading quarantine management: " + casingName, e);
            return;
        }
        quarantineJTabbedPane.addTab(NAME_ALL_ACCOUNTS, null, quarantineAllJPanel);

        // QUARANTINABLE ADDRESSES //////
        String quarantinableAddressesJPanelName = "com.metavize.tran.mail.gui.QuarantinableAddressesJPanel";
        JPanel quarantinableAddressesJPanel = null;
        try{
            Class objectClass = Util.getClassLoader().loadClass( quarantinableAddressesJPanelName, transformDesc );
            Constructor objectConstructor = objectClass.getConstructor(new Class[]{TransformContext.class});
            quarantinableAddressesJPanel = (JPanel) objectConstructor.newInstance(transformContext);
        }
        catch(Exception e){
            Util.handleExceptionNoRestart("Error loading quarantinable addresses: " + casingName, e);
            return;
        }
        quarantineJTabbedPane.addTab(NAME_QUARANTINABLE_ADDRESSES, null, quarantinableAddressesJPanel);
	addSavable(NAME_QUARANTINABLE_ADDRESSES, (Savable) quarantinableAddressesJPanel);
	addRefreshable(NAME_QUARANTINABLE_ADDRESSES, (Refreshable) quarantinableAddressesJPanel);

        // QUARANTINABLE FORWARDS //////
        String quarantinableForwardsJPanelName = "com.metavize.tran.mail.gui.QuarantinableForwardsJPanel";
        JPanel quarantinableForwardsJPanel = null;
        try{
            Class objectClass = Util.getClassLoader().loadClass( quarantinableForwardsJPanelName, transformDesc );
            Constructor objectConstructor = objectClass.getConstructor(new Class[]{TransformContext.class});
            quarantinableForwardsJPanel = (JPanel) objectConstructor.newInstance(transformContext);
        }
        catch(Exception e){
            Util.handleExceptionNoRestart("Error loading quarantinable forwards: " + casingName, e);
            return;
        }
        quarantineJTabbedPane.addTab(NAME_QUARANTINABLE_FORWARDS, null, quarantinableForwardsJPanel);
	addSavable(NAME_QUARANTINABLE_FORWARDS, (Savable) quarantinableForwardsJPanel);
	addRefreshable(NAME_QUARANTINABLE_FORWARDS, (Refreshable) quarantinableForwardsJPanel);

        // QUARANTINE GENERAL SETTINGS //////
        String quarantineGeneralSettingsJPanelName = "com.metavize.tran.mail.gui.QuarantineGeneralSettingsJPanel";
        JPanel quarantineGeneralSettingsJPanel = null;
        try{
            Class objectClass = Util.getClassLoader().loadClass( quarantineGeneralSettingsJPanelName, transformDesc );
            Constructor objectConstructor = objectClass.getConstructor(new Class[]{TransformContext.class});
            quarantineGeneralSettingsJPanel = (JPanel) objectConstructor.newInstance(transformContext);
        }
        catch(Exception e){
            Util.handleExceptionNoRestart("Error loading quarantine general settings: " + casingName, e);
            return;
        }
        quarantineJTabbedPane.addTab(NAME_GENERAL_SETTINGS, null, quarantineGeneralSettingsJPanel);
	addSavable(NAME_GENERAL_SETTINGS, (Savable) quarantineGeneralSettingsJPanel);
	addRefreshable(NAME_GENERAL_SETTINGS, (Refreshable) quarantineGeneralSettingsJPanel);
    }
    
    protected void sendSettings(Object settings) throws Exception { }
    protected void refreshSettings() {  }

}
