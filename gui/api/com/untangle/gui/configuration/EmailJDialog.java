/*
 * $HeadURL:$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.gui.configuration;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;

import com.untangle.gui.node.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.uvm.node.*;


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

        // EMAIL NODE SETTINGS //
        if( emailCompoundSettings.getMailNodeCompoundSettings() != null ){

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
        if( emailCompoundSettings.getMailNodeCompoundSettings() != null ){
            emailCompoundSettings.loadSafelists();
            emailCompoundSettings.loadSafelistCounts();
            emailCompoundSettings.loadInboxList();
        }

    }
}
