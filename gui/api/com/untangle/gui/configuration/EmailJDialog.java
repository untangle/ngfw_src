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

package com.untangle.gui.configuration;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;

import com.untangle.gui.transform.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.mvvm.tran.*;


public class EmailJDialog extends MConfigJDialog {

    private static final String NAME_EMAIL_CONFIG            = "Email Config";
    private static final String NAME_OUTGOING_SETTINGS       = "Outgoing Server";
    private static final String NAME_SAFE_LIST               = "From-Safe List";
    private static final String NAME_SAFE_LIST_GLOBAL        = "Global";
    private static final String NAME_SAFE_LIST_USER          = "Per User";
    private static final String NAME_QUARANTINE_SETTINGS     = "Quarantine";
    private static final String NAME_ALL_ACCOUNTS            = "Release & Purge";
    private static final String NAME_QUARANTINABLE_ADDRESSES = "Quarantinable Addresses";
    private static final String NAME_QUARANTINABLE_FORWARDS  = "Quarantinable Forwards";
    private static final String NAME_GENERAL_SETTINGS        = "General Settings";

    private EmailCompoundSettings emailCompoundSettings;

    public EmailJDialog( Frame parentFrame ) {
        super(parentFrame);
        setTitle(NAME_EMAIL_CONFIG);
        setHelpSource("email_config");
        compoundSettings = new EmailCompoundSettings();
        emailCompoundSettings = (EmailCompoundSettings) compoundSettings;
        INSTANCE = this;
    }

    protected Dimension getMinSize(){
        return new Dimension(640, 550);
    }


    private static EmailJDialog INSTANCE;
    public static EmailJDialog instance(){ return INSTANCE; }

    protected void generateGui(){
        // OUTGOING SERVER /////
        EmailOutgoingJPanel emailOutgoingJPanel = new EmailOutgoingJPanel();
        addScrollableTab(null, NAME_OUTGOING_SETTINGS, null, emailOutgoingJPanel, false, true);
        addSavable(NAME_OUTGOING_SETTINGS, emailOutgoingJPanel );
        addRefreshable(NAME_OUTGOING_SETTINGS, emailOutgoingJPanel );
        emailOutgoingJPanel.setSettingsChangedListener(this);

        // EMAIL TRANSFORM SETTINGS //
        if( emailCompoundSettings.getMailTransformCompoundSettings() != null ){

            // SAFELIST GLOBAL & USER //////
            JTabbedPane safelistJTabbedPane = addTabbedPane(NAME_SAFE_LIST, null);

            Component whitelistGlobalComponent = emailCompoundSettings.getSafelistGlobalComponent();
            safelistJTabbedPane.addTab(NAME_SAFE_LIST_GLOBAL, null, whitelistGlobalComponent);
            addSavable(NAME_SAFE_LIST_GLOBAL, (Savable) whitelistGlobalComponent);
            addRefreshable(NAME_SAFE_LIST_GLOBAL, (Refreshable) whitelistGlobalComponent);
            ((MEditTableJPanel)whitelistGlobalComponent).setSettingsChangedListener(this);

            Component whitelistAllUsersComponent = emailCompoundSettings.getSafelistAllUsersComponent();
            safelistJTabbedPane.addTab(NAME_SAFE_LIST_USER, null, whitelistAllUsersComponent);
            addRefreshable(NAME_SAFE_LIST_USER, (Refreshable) whitelistAllUsersComponent);

            // QUARANTINE ///////
            JTabbedPane quarantineJTabbedPane = addTabbedPane(NAME_QUARANTINE_SETTINGS, null);

            // QUARANTINE RELEASE & PURGE //////
            Component quarantineAllComponent = emailCompoundSettings.getQuarantineReleaseAndPurgeComponent();
            quarantineJTabbedPane.addTab(NAME_ALL_ACCOUNTS, null, quarantineAllComponent);
            addRefreshable(NAME_ALL_ACCOUNTS, (Refreshable) quarantineAllComponent);

            // QUARANTINABLE ADDRESSES //////
            Component quarantinableAddressesComponent = emailCompoundSettings.getQuarantinableAddressesComponent();
            quarantineJTabbedPane.addTab(NAME_QUARANTINABLE_ADDRESSES, null, quarantinableAddressesComponent);
            addSavable(NAME_QUARANTINABLE_ADDRESSES, (Savable) quarantinableAddressesComponent);
            addRefreshable(NAME_QUARANTINABLE_ADDRESSES, (Refreshable) quarantinableAddressesComponent);
            ((MEditTableJPanel)quarantinableAddressesComponent).setSettingsChangedListener(this);

            // QUARANTINABLE FORWARDS //////
            Component quarantinableForwardsComponent = emailCompoundSettings.getQuarantinableForwardsComponent();
            quarantineJTabbedPane.addTab(NAME_QUARANTINABLE_FORWARDS, null, quarantinableForwardsComponent);
            addSavable(NAME_QUARANTINABLE_FORWARDS, (Savable) quarantinableForwardsComponent);
            addRefreshable(NAME_QUARANTINABLE_FORWARDS, (Refreshable) quarantinableForwardsComponent);
            ((MEditTableJPanel)quarantinableForwardsComponent).setSettingsChangedListener(this);

            // QUARANTINE GENERAL SETTINGS //////
            Component quarantineGeneralSettingsComponent = emailCompoundSettings.getQuarantineGeneralSettingsComponent();
            quarantineJTabbedPane.addTab(NAME_GENERAL_SETTINGS, null, quarantineGeneralSettingsComponent);
            addSavable(NAME_GENERAL_SETTINGS, (Savable) quarantineGeneralSettingsComponent);
            addRefreshable(NAME_GENERAL_SETTINGS, (Refreshable) quarantineGeneralSettingsComponent);
            ((MEditTableJPanel)quarantineGeneralSettingsComponent).setSettingsChangedListener(this);
        }
    }

    protected void refreshAll() throws Exception{
        super.refreshAll();
        if( emailCompoundSettings.getMailTransformCompoundSettings() != null ){
            emailCompoundSettings.loadSafelists();
            emailCompoundSettings.loadSafelistCounts();
            emailCompoundSettings.loadInboxList();
        }

    }
}
