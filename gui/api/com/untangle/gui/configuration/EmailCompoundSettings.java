
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

import com.untangle.gui.util.Util;
import com.untangle.gui.transform.CompoundSettings;
import com.untangle.mvvm.MailSettings;

import java.awt.Component;
import java.util.List;

public class EmailCompoundSettings implements CompoundSettings {

    // MAIL SETTINGS //
    private MailSettings mailSettings;
    public MailSettings getMailSettings(){ return mailSettings; }

    // MAIL TRANSFORM SETTINGS //
    private CompoundSettings mailTransformCompoundSettings;
    public CompoundSettings getMailTransformCompoundSettings(){ return mailTransformCompoundSettings; }
    public void loadSafelists() throws Exception {
        mailTransformCompoundSettings.getClass().getDeclaredMethod("loadSafelists", new Class[]{}).invoke(mailTransformCompoundSettings);
    }
    public void loadSafelistCounts() throws Exception {
	mailTransformCompoundSettings.getClass().getDeclaredMethod("loadSafelistCounts", new Class[]{}).invoke(mailTransformCompoundSettings);
    }
    public void loadInboxList() throws Exception {
	mailTransformCompoundSettings.getClass().getDeclaredMethod("loadInboxList", new Class[]{}).invoke(mailTransformCompoundSettings);
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
	if(mailTransformCompoundSettings != null)
	    mailTransformCompoundSettings.save();
    }

    public void refresh() throws Exception {
	mailSettings = Util.getAdminManager().getMailSettings();

	if(mailTransformCompoundSettings == null){
	    mailTransformCompoundSettings = Util.getCompoundSettings("com.untangle.tran.mail.gui.MailTransformCompoundSettings", "mail-casing");
	}
	if(mailTransformCompoundSettings != null){
	    mailTransformCompoundSettings.refresh();

	    safelistAllUsersComponent = Util.getSettingsComponent("com.untangle.tran.mail.gui.WhitelistAllUsersJPanel", "mail-casing");
	    safelistGlobalComponent = Util.getSettingsComponent("com.untangle.tran.mail.gui.WhitelistGlobalJPanel", "mail-casing");
	    quarantineReleaseAndPurgeComponent = Util.getSettingsComponent("com.untangle.tran.mail.gui.QuarantineAllUsersJPanel", "mail-casing");
	    quarantinableAddressesComponent = Util.getSettingsComponent("com.untangle.tran.mail.gui.QuarantinableAddressesJPanel", "mail-casing");
	    quarantinableForwardsComponent = Util.getSettingsComponent("com.untangle.tran.mail.gui.QuarantinableForwardsJPanel", "mail-casing");
	    quarantineGeneralSettingsComponent = Util.getSettingsComponent("com.untangle.tran.mail.gui.QuarantineGeneralSettingsJPanel", "mail-casing");

	}
    }

    public void validate() throws Exception {

    }
    

}
