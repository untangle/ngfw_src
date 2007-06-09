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

import com.untangle.gui.node.CompoundSettings;
import com.untangle.gui.node.MCasingJPanel;
import com.untangle.gui.util.Util;
import com.untangle.uvm.networking.AccessSettings;
import com.untangle.uvm.networking.MiscSettings;
import com.untangle.uvm.networking.NetworkSpacesSettings;


public class MaintenanceCompoundSettings implements CompoundSettings {

    // ACCESS SETTINGS //
    private AccessSettings accessSettings;
    public AccessSettings getAccessSettings(){ return accessSettings; }

    // MISC SETTINGS //
    private MiscSettings miscSettings;
    public MiscSettings getMiscSettings() { return miscSettings; }

    // NETWORK SETTINGS //
    private NetworkSpacesSettings networkSettings;
    public NetworkSpacesSettings getNetworkSettings(){ return networkSettings; }

    // MAIL NODE SETTINGS //
    private CompoundSettings mailNodeCompoundSettings;
    public CompoundSettings getMailNodeCompoundSettings(){ return mailNodeCompoundSettings; }

    // HTTP NODE SETTINGS //
    private CompoundSettings httpNodeCompoundSettings;
    public CompoundSettings getHttpNodeCompoundSettings(){ return httpNodeCompoundSettings; }

    // FTP NODE SETTINGS //
    private CompoundSettings ftpNodeCompoundSettings;
    public CompoundSettings getFtpNodeCompoundSettings(){ return ftpNodeCompoundSettings; }

    private MCasingJPanel[] casingJPanels;
    public MCasingJPanel[] getCasingJPanels(){ return casingJPanels; }

    public void save() throws Exception {
        Util.getNetworkManager().setSettings(accessSettings,miscSettings,networkSettings);

        if(mailNodeCompoundSettings != null){
            mailNodeCompoundSettings.save();
        }
        if(httpNodeCompoundSettings != null){
            httpNodeCompoundSettings.save();
        }
        if(ftpNodeCompoundSettings != null){
            ftpNodeCompoundSettings.save();
        }
    }

    public void refresh() throws Exception {
        Util.getNetworkManager().updateLinkStatus();
        accessSettings = Util.getNetworkManager().getAccessSettings();
        miscSettings = Util.getNetworkManager().getMiscSettings();
        networkSettings = Util.getNetworkManager().getNetworkSettings();

        casingJPanels = Util.getPolicyStateMachine().loadAllCasings(true);

        if(mailNodeCompoundSettings == null){
            mailNodeCompoundSettings = Util.getCompoundSettings("com.untangle.node.mail.gui.MailNodeCompoundSettings", "mail-casing");
        }
        if(mailNodeCompoundSettings != null)
            mailNodeCompoundSettings.refresh();

        if(httpNodeCompoundSettings == null){
            httpNodeCompoundSettings = Util.getCompoundSettings("com.untangle.node.http.gui.HttpNodeCompoundSettings", "http-casing");
        }
        if(httpNodeCompoundSettings != null)
            httpNodeCompoundSettings.refresh();

        if(ftpNodeCompoundSettings == null){
            ftpNodeCompoundSettings = Util.getCompoundSettings("com.untangle.node.ftp.gui.FtpNodeCompoundSettings", "ftp-casing");
        }
        if(ftpNodeCompoundSettings != null)
            ftpNodeCompoundSettings.refresh();
    }

    public void validate() throws Exception {
        accessSettings.validate();
        miscSettings.validate();
        System.err.println( "need validation for network settings" );
        //networkSettings.validate();
    }

}
