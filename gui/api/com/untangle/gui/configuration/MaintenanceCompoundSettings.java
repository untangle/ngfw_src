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
import com.untangle.gui.transform.MCasingJPanel;
import com.untangle.mvvm.networking.AccessSettings;
import com.untangle.mvvm.networking.MiscSettings;
import com.untangle.mvvm.networking.NetworkSpacesSettings;
import com.untangle.mvvm.security.Tid;
import com.untangle.mvvm.tran.TransformContext;
import com.untangle.mvvm.tran.Transform;

import java.util.List;

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

    // MAIL TRANSFORM SETTINGS //
    private CompoundSettings mailTransformCompoundSettings;
    public CompoundSettings getMailTransformCompoundSettings(){ return mailTransformCompoundSettings; }

    // HTTP TRANSFORM SETTINGS //
    private CompoundSettings httpTransformCompoundSettings;
    public CompoundSettings getHttpTransformCompoundSettings(){ return httpTransformCompoundSettings; }

    // FTP TRANSFORM SETTINGS //
    private CompoundSettings ftpTransformCompoundSettings;
    public CompoundSettings getFtpTransformCompoundSettings(){ return ftpTransformCompoundSettings; }

    private MCasingJPanel[] casingJPanels;
    public MCasingJPanel[] getCasingJPanels(){ return casingJPanels; }

    public void save() throws Exception {
	Util.getNetworkManager().setSettings(accessSettings,miscSettings,networkSettings);

	if(mailTransformCompoundSettings != null){
	    mailTransformCompoundSettings.save();
	}
	if(httpTransformCompoundSettings != null){
	    httpTransformCompoundSettings.save();
	}
	if(ftpTransformCompoundSettings != null){
	    ftpTransformCompoundSettings.save();
	}
    }

    public void refresh() throws Exception {
        Util.getNetworkManager().updateLinkStatus();
        accessSettings = Util.getNetworkManager().getAccessSettings();
        miscSettings = Util.getNetworkManager().getMiscSettings();
	networkSettings = Util.getNetworkManager().getNetworkSettings();

	casingJPanels = Util.getPolicyStateMachine().loadAllCasings(true);

	if(mailTransformCompoundSettings == null){
	    mailTransformCompoundSettings = Util.getCompoundSettings("com.untangle.tran.mail.gui.MailTransformCompoundSettings", "mail-casing");
	}
	if(mailTransformCompoundSettings != null)
	    mailTransformCompoundSettings.refresh();

	if(httpTransformCompoundSettings == null){
	    httpTransformCompoundSettings = Util.getCompoundSettings("com.untangle.tran.http.gui.HttpTransformCompoundSettings", "http-casing");
	}
	if(httpTransformCompoundSettings != null)
	    httpTransformCompoundSettings.refresh();

	if(ftpTransformCompoundSettings == null){
	    ftpTransformCompoundSettings = Util.getCompoundSettings("com.untangle.tran.ftp.gui.FtpTransformCompoundSettings", "ftp-casing");
	}
	if(ftpTransformCompoundSettings != null)
	    ftpTransformCompoundSettings.refresh();
    }

    public void validate() throws Exception {
        accessSettings.validate();
        miscSettings.validate();
        System.err.println( "need validation for network settings" );
        //networkSettings.validate();
    }

}
