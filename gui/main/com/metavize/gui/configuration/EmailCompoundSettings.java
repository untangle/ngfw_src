
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

import com.metavize.gui.util.Util;
import com.metavize.gui.transform.CompoundSettings;
import com.metavize.mvvm.MailSettings;

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
    private Component safelistComponent;
    public Component getSafelistComponent(){ return safelistComponent; }

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
	    mailTransformCompoundSettings = Util.getCompoundSettings("com.metavize.tran.mail.gui.MailTransformCompoundSettings", "mail-casing");
	}
	if(mailTransformCompoundSettings != null){
	    mailTransformCompoundSettings.refresh();

	    safelistComponent = Util.getSettingsComponent("com.metavize.tran.mail.gui.WhitelistAllJPanel", "mail-casing");
	    quarantineReleaseAndPurgeComponent = Util.getSettingsComponent("com.metavize.tran.mail.gui.QuarantineAllUsersJPanel", "mail-casing");
	    quarantinableAddressesComponent = Util.getSettingsComponent("com.metavize.tran.mail.gui.QuarantinableAddressesJPanel", "mail-casing");
	    quarantinableForwardsComponent = Util.getSettingsComponent("com.metavize.tran.mail.gui.QuarantinableForwardsJPanel", "mail-casing");
	    quarantineGeneralSettingsComponent = Util.getSettingsComponent("com.metavize.tran.mail.gui.QuarantineGeneralSettingsJPanel", "mail-casing");

	}
    }

    public void validate() throws Exception {

    }
    

}
