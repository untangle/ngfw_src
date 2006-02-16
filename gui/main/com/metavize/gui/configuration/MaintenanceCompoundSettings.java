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
import com.metavize.gui.transform.MCasingJPanel;
import com.metavize.mvvm.NetworkingConfiguration;
import com.metavize.mvvm.networking.NetworkSpacesSettings;
import com.metavize.mvvm.security.Tid;
import com.metavize.mvvm.tran.TransformContext;
import com.metavize.mvvm.tran.Transform;

import java.util.List;

public class MaintenanceCompoundSettings implements CompoundSettings {

    // NETWORKING CONFIGURATION //
    private NetworkingConfiguration networkingConfiguration;
    public NetworkingConfiguration getNetworkingConfiguration(){ return networkingConfiguration; }

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
	Util.getNetworkingManager().set(networkingConfiguration);
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
	networkingConfiguration = Util.getNetworkingManager().get();
	networkSettings = Util.getNetworkManager().getNetworkSettings();

	casingJPanels = Util.getPolicyStateMachine().loadAllCasings(true);

	if(mailTransformCompoundSettings == null){
	    mailTransformCompoundSettings = Util.getCompoundSettings("com.metavize.tran.mail.gui.MailTransformCompoundSettings", "mail-casing");
	}
	if(mailTransformCompoundSettings != null)
	    mailTransformCompoundSettings.refresh();

	if(httpTransformCompoundSettings == null){
	    httpTransformCompoundSettings = Util.getCompoundSettings("com.metavize.tran.http.gui.HttpTransformCompoundSettings", "http-casing");
	}
	if(httpTransformCompoundSettings != null)
	    httpTransformCompoundSettings.refresh();

	if(ftpTransformCompoundSettings == null){
	    ftpTransformCompoundSettings = Util.getCompoundSettings("com.metavize.tran.ftp.gui.FtpTransformCompoundSettings", "ftp-casing");
	}
	if(ftpTransformCompoundSettings != null)
	    ftpTransformCompoundSettings.refresh();
    }

    public void validate() throws Exception {

    }

}
