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

import com.metavize.gui.util.Util;
import com.metavize.gui.transform.CompoundSettings;
import com.metavize.tran.mail.papi.MailTransform;
import com.metavize.tran.mail.papi.MailTransformSettings;
import com.metavize.tran.mail.papi.quarantine.Inbox;
import com.metavize.tran.mail.papi.quarantine.InboxIndex;
import com.metavize.tran.mail.papi.quarantine.QuarantineSettings;
import com.metavize.tran.mail.papi.quarantine.QuarantineMaintenenceView;
import com.metavize.tran.mail.papi.safelist.SafelistAdminView;

import java.awt.Component;
import java.util.List;

public class MailTransformCompoundSettings implements CompoundSettings {

    // MAIL TRANSFORM SETTINGS //
    private MailTransformSettings mailTransformSettings;
    public MailTransformSettings getMailTransformSettings(){ return mailTransformSettings; }
    private MailTransform mailTransform;

    // GENERAL SETTINGS //
    private Component generalSettingsComponent;
    public Component getGeneralSettingsComponent(){ return generalSettingsComponent; }

    // QUARANTINE SETTINGS //
    private QuarantineSettings quarantineSettings;
    public QuarantineSettings getQuarantineSettings(){ return quarantineSettings; }

    // QUARANTINE MAINTENANCE VIEW //
    private QuarantineMaintenenceView quarantineMaintenanceView;
    public QuarantineMaintenenceView getQuarantineMaintenanceView(){ return quarantineMaintenanceView; }

    // SAFELIST ADMIN VIEW //
    private SafelistAdminView safelistAdminView;
    public SafelistAdminView getSafelistAdminView(){ return safelistAdminView; }
    private String[] safelistContents;
    private List<String> safelists;
    private int[] safelistCounts;
    private InboxIndex inboxIndex;
    private List<Inbox> inboxList;
    public String[] getSafelistContents(){ return safelistContents; }
    public List<String> getSafelists(){ return safelists; }
    public int[] getSafelistCounts(){ return safelistCounts; }
    public List<Inbox> getInboxList() {
	return inboxList;
    }
    public InboxIndex getInboxIndex(){
	return inboxIndex;
    }
    public void loadSafelists() throws Exception {
	safelists = safelistAdminView.listSafelists();
    }
    public void loadSafelistCounts() throws Exception {
	safelistCounts = new int[safelists.size()];
	int i=0;
	for(String account : safelists){
	    safelistCounts[i] = safelistAdminView.getSafelistCnt(account);
	    i++;
	}
    }
    public void loadSafelistContents(String account) throws Exception { safelistContents = safelistAdminView.getSafelistContents(account); }
    public void loadInboxIndex(String account) throws Exception { inboxIndex = quarantineMaintenanceView.getInboxIndex(account); }
    public void loadInboxList() throws Exception { inboxList = quarantineMaintenanceView.listInboxes(); }

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
	mailTransformSettings.setQuarantineSettings(quarantineSettings);
	mailTransform.setMailTransformSettings(mailTransformSettings);
    }

    public void refresh() throws Exception {
	if(mailTransform == null)
	    mailTransform = (MailTransform) Util.getTransform("mail-casing");
	mailTransformSettings = mailTransform.getMailTransformSettings();
	quarantineSettings = mailTransformSettings.getQuarantineSettings();

	if(quarantineMaintenanceView == null)
	    quarantineMaintenanceView = mailTransform.getQuarantineMaintenenceView();
	if(safelistAdminView == null)
	    safelistAdminView = mailTransform.getSafelistAdminView();

	if(generalSettingsComponent == null)
	    generalSettingsComponent = Util.getSettingsComponent("com.metavize.tran.mail.gui.MCasingJPanel", "mail-casing");
	if(safelistComponent == null)
	    safelistComponent = Util.getSettingsComponent("com.metavize.tran.mail.gui.WhitelistAllJPanel", "mail-casing");
	if(quarantineReleaseAndPurgeComponent == null)
	    quarantineReleaseAndPurgeComponent = Util.getSettingsComponent("com.metavize.tran.mail.gui.QuarantineAllJPanel", "mail-casing");
	if(quarantinableAddressesComponent == null)
	    quarantinableAddressesComponent = Util.getSettingsComponent("com.metavize.tran.mail.gui.QuarantinableAddressesJPanel", "mail-casing");
	if(quarantinableForwardsComponent == null)
	    quarantinableForwardsComponent = Util.getSettingsComponent("com.metavize.tran.mail.gui.QuarantinableForwardsJPanel", "mail-casing");
	if(quarantineGeneralSettingsComponent == null)
	    quarantineGeneralSettingsComponent = Util.getSettingsComponent("com.metavize.tran.mail.gui.QuarantineGeneralSettingsJPanel", "mail-casing");
    }

    public void validate() throws Exception {

    }

}
