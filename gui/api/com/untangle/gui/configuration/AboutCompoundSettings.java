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

		if ( Util.getIsPremium() ) {
        	licenseURL = Util.getClassLoader().getResource("LicenseProfessional.txt");
		}
		else {
        	licenseURL = Util.getClassLoader().getResource("LicenseStandard.txt");
		}

        timeZone = Util.getAdminManager().getTimeZone();
        date = Util.getAdminManager().getDate();
        brandingSettings = Util.getBrandingManager().getBrandingSettings();
    }

    public void validate() throws Exception {

    }

}
