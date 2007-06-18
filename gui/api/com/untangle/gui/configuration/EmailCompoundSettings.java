
/*
 * $HeadURL$
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
