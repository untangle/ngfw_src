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
import com.metavize.mvvm.security.RegistrationInfo;
import com.metavize.mvvm.toolbox.MackageDesc;

import java.net.URL;

public class AboutCompoundSettings implements CompoundSettings {

    // REGISTRATION INFO //
    private RegistrationInfo registrationInfo;
    public RegistrationInfo getRegistrationInfo(){ return registrationInfo; }
    public void setRegistrationInfo(RegistrationInfo riIn){ registrationInfo = riIn; }

    // INSTALLED VERISON //
    private String installedVersion;
    public String getInstalledVersion(){ return installedVersion; }

    // LICENSE //
    private URL licenseURL;
    public URL getLicenseURL(){ return licenseURL; }

    // ABOUT //
    private String aboutText = "<br><br><b>Readme:</b> http://www.metavize.com/egquickstart<br><br><b>Website: </b>http://www.metavize.com";
    public String getAboutText(){ return aboutText; }
    
    public void save() throws Exception {
	Util.getAdminManager().setRegistrationInfo(registrationInfo);
    }

    public void refresh() throws Exception {
	registrationInfo = Util.getAdminManager().getRegistrationInfo();
	MackageDesc mackageDesc = Util.getToolboxManager().mackageDesc("mvvm");
	if( mackageDesc == null )
	    installedVersion = "unknown";
	else
	    installedVersion = mackageDesc.getInstalledVersion();

	licenseURL = Util.getClassLoader().getResource("License.txt");
    }

    public void validate() throws Exception {

    }

}
