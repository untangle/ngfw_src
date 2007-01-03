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
import com.untangle.mvvm.networking.RemoteSettings;
import com.untangle.mvvm.networking.NetworkSpacesSettings;
import com.untangle.mvvm.security.Tid;
import com.untangle.mvvm.tran.TransformContext;
import com.untangle.mvvm.tran.Transform;

import java.util.List;

public class MaintenanceCompoundSettings implements CompoundSettings {

    // NETWORKING CONFIGURATION //
    private RemoteSettings remoteSettings;
    public RemoteSettings getRemoteSettings(){ return remoteSettings; }

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
        /* RBS: !!! It is important that the remote settings are saved before the 
         * the network settings, if they are not, the post configuration script
         * may or may not be executed. */
	Util.getNetworkManager().setRemoteSettings(remoteSettings);
	Util.getNetworkManager().setNetworkSettings(networkSettings);

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
	remoteSettings  = Util.getNetworkManager().getRemoteSettings();
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

    }

}
