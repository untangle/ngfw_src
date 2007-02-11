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
import com.untangle.mvvm.security.RegistrationInfo;
import com.untangle.mvvm.toolbox.MackageDesc;

import java.util.TimeZone;
import java.util.Date;
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
    
    // TIMEZONE //
    private TimeZone timeZone;
    private Date date;
    public TimeZone getTimeZone(){ return timeZone; }
    public void setTimeZone(TimeZone tzIn){ timeZone = tzIn; };
    public Date getDate(){ return date; }
	
    public void save() throws Exception {
	Util.getAdminManager().setRegistrationInfo(registrationInfo);
	Util.getAdminManager().setTimeZone(timeZone);
    }

    public void refresh() throws Exception {
	registrationInfo = Util.getAdminManager().getRegistrationInfo();
	if( registrationInfo == null ){ // for upgrading pre 3.2 boxes
	    registrationInfo = new RegistrationInfo();
	}
	MackageDesc mackageDesc = Util.getToolboxManager().mackageDesc("mvvm");
	if( mackageDesc == null )
	    installedVersion = "unknown";
	else
	    installedVersion = mackageDesc.getInstalledVersion();
	
	licenseURL = Util.getClassLoader().getResource("License.txt");
	timeZone = Util.getAdminManager().getTimeZone();
	date = Util.getAdminManager().getDate();
    }

    public void validate() throws Exception {

    }

}
