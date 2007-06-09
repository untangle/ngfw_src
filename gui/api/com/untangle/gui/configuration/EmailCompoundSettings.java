
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

import com.untangle.gui.node.CompoundSettings;
import com.untangle.gui.util.Util;
import com.untangle.uvm.MailSettings;

public class EmailCompoundSettings implements CompoundSettings {

    // MAIL SETTINGS //
    private MailSettings mailSettings;
    public MailSettings getMailSettings(){ return mailSettings; }

    // MAIL NODE SETTINGS //
    private CompoundSettings mailNodeCompoundSettings;
    public CompoundSettings getMailNodeCompoundSettings(){ return mailNodeCompoundSettings; }
    public void loadSafelists() throws Exception {
        mailNodeCompoundSettings.getClass().getDeclaredMethod("loadSafelists", new Class[]{}).invoke(mailNodeCompoundSettings);
    }
    public void loadSafelistCounts() throws Exception {
        mailNodeCompoundSettings.getClass().getDeclaredMethod("loadSafelistCounts", new Class[]{}).invoke(mailNodeCompoundSettings);
    }
    public void loadInboxList() throws Exception {
        mailNodeCompoundSettings.getClass().getDeclaredMethod("loadInboxList", new Class[]{}).invoke(mailNodeCompoundSettings);
    }





    // QUARANTINE SAFELIST CONTROLS //////
    private Component safelistGlobalComponent;
    public Component getSafelistGlobalComponent(){ return safelistGlobalComponent; }
    private Component safelistAllUsersComponent;
    public Component getSafelistAllUsersComponent(){ return safelistAllUsersComponent; }

    // QUARANTINE RELEASE & PURGE //
    private Component quarantineReleaseAndPurgeComponent;
    public Component getQuarantineReleaseAndPurgeComponent(){ return quarantineReleaseAndPurgeComponent; }

    // QUARANTINABLE ADDRESSES //
    private Component quarantinableAddressesComponent;
    public Component getQuarantinableAddressesComponent(){ return quarantinableAddressesComponent; }

    // QUARANTINABLE FORWARDS //
    private Component quarantinableForwardsComponent;
    public Component getQuarantinableForwardsComponent(){ return quarantinableForwardsComponent; }

    // QUARANTINE GENERAL SETTINGS //
    private Component quarantineGeneralSettingsComponent;
    public Component getQuarantineGeneralSettingsComponent(){ return quarantineGeneralSettingsComponent; }

    public void save() throws Exception {
        Util.getAdminManager().setMailSettings(mailSettings);
        if(mailNodeCompoundSettings != null)
            mailNodeCompoundSettings.save();
    }

    public void refresh() throws Exception {
        mailSettings = Util.getAdminManager().getMailSettings();

        if(mailNodeCompoundSettings == null){
            mailNodeCompoundSettings = Util.getCompoundSettings("com.untangle.node.mail.gui.MailNodeCompoundSettings", "mail-casing");
        }
        if(mailNodeCompoundSettings != null){
            mailNodeCompoundSettings.refresh();

            safelistAllUsersComponent = Util.getSettingsComponent("com.untangle.node.mail.gui.WhitelistAllUsersJPanel", "mail-casing");
            safelistGlobalComponent = Util.getSettingsComponent("com.untangle.node.mail.gui.WhitelistGlobalJPanel", "mail-casing");
            quarantineReleaseAndPurgeComponent = Util.getSettingsComponent("com.untangle.node.mail.gui.QuarantineAllUsersJPanel", "mail-casing");
            quarantinableAddressesComponent = Util.getSettingsComponent("com.untangle.node.mail.gui.QuarantinableAddressesJPanel", "mail-casing");
            quarantinableForwardsComponent = Util.getSettingsComponent("com.untangle.node.mail.gui.QuarantinableForwardsJPanel", "mail-casing");
            quarantineGeneralSettingsComponent = Util.getSettingsComponent("com.untangle.node.mail.gui.QuarantineGeneralSettingsJPanel", "mail-casing");

        }
    }

    public void validate() throws Exception {

    }


}
