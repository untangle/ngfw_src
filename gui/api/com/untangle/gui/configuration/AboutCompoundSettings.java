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

import java.net.URL;
import java.util.Date;
import java.util.TimeZone;

import com.untangle.gui.node.CompoundSettings;
import com.untangle.gui.util.Util;
import com.untangle.uvm.security.RegistrationInfo;
import com.untangle.uvm.toolbox.MackageDesc;
import com.untangle.uvm.BrandingManager;
import com.untangle.uvm.BrandingSettings;

public class AboutCompoundSettings implements CompoundSettings {

    // REGISTRATION INFO //
    private RegistrationInfo registrationInfo;
    public RegistrationInfo getRegistrationInfo(){ return registrationInfo; }
    public void setRegistrationInfo(RegistrationInfo riIn){ registrationInfo = riIn; }

    // INSTALLED VERISON //
    private String installedVersion;
    public String getInstalledVersion(){ return installedVersion; }

    // KEY
    private String activationKey;
    public String getActivationKey(){ return activationKey; }

    // LICENSE //
    private URL licenseURL;
    public URL getLicenseURL(){ return licenseURL; }

    // TIMEZONE //
    private TimeZone timeZone;
    private Date date;
    public TimeZone getTimeZone(){ return timeZone; }
    public void setTimeZone(TimeZone tzIn){ timeZone = tzIn; };
    public Date getDate(){ return date; }

    // BRANDING //
    private BrandingSettings brandingSettings;
    public BrandingSettings getBrandingSettings(){ return brandingSettings; }
    public void setBrandingSettings(BrandingSettings bsIn){ brandingSettings = bsIn; }
    
    public void save() throws Exception {
        Util.getAdminManager().setRegistrationInfo(registrationInfo);
        Util.getAdminManager().setTimeZone(timeZone);
        Util.getBrandingManager().setBrandingSettings(brandingSettings);        
    }

    public void refresh() throws Exception {
        registrationInfo = Util.getAdminManager().getRegistrationInfo();
        if( registrationInfo == null ){ // for upgrading pre 3.2 boxes
            registrationInfo = new RegistrationInfo();
        }
        MackageDesc mackageDesc = Util.getToolboxManager().mackageDesc("uvm");
        if( mackageDesc == null )
            installedVersion = "unknown";
        else
            installedVersion = mackageDesc.getInstalledVersion();
        activationKey = Util.getUvmContext().getActivationKey();
        licenseURL = Util.getClassLoader().getResource("License.txt");
        timeZone = Util.getAdminManager().getTimeZone();
        date = Util.getAdminManager().getDate();
        brandingSettings = Util.getBrandingManager().getBrandingSettings();
    }

    public void validate() throws Exception {

    }

}
