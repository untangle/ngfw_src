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

package com.untangle.mvvm.networking.internal;

import com.untangle.mvvm.networking.AccessSettings;

import com.untangle.mvvm.tran.HostName;
import com.untangle.mvvm.tran.IPaddr;

/** These are settings related to remote access to the untangle.
 *  These are related to controlling access to resources on
 * the local server. */
public class AccessSettingsInternal
{
    private final boolean isSshEnabled;

    private final boolean isInsideInsecureEnabled;
    private final boolean isOutsideAccessEnabled;
    private final boolean isOutsideAccessRestricted;
    
    private final IPaddr outsideNetwork;
    private final IPaddr outsideNetmask;

    private final boolean isOutsideAdministrationEnabled;
    private final boolean isOutsideQuarantineEnabled;
    private final boolean isOutsideReportingEnabled;
    
    private AccessSettingsInternal( AccessSettings settings )
    {
        this.isSshEnabled = settings.getIsSshEnabled();
        this.isInsideInsecureEnabled = settings.getIsInsideInsecureEnabled();
        this.isOutsideAccessEnabled = settings.getIsOutsideAccessEnabled();
        this.isOutsideAccessRestricted = settings.getIsOutsideAccessRestricted();
        this.outsideNetwork = settings.getOutsideNetwork();
        this.outsideNetmask = settings.getOutsideNetmask();

        this.isOutsideAdministrationEnabled = settings.getIsOutsideAdministrationEnabled();
        this.isOutsideQuarantineEnabled = settings.getIsOutsideQuarantineEnabled();
        this.isOutsideReportingEnabled = settings.getIsOutsideReportingEnabled();
    }
    
    /* Get whether or not ssh is enabled. */
    public boolean getIsSshEnabled()
    {
        return this.isSshEnabled;
    }
            
    /** True if insecure access from the inside is enabled. */
    public boolean getIsInsideInsecureEnabled()
    {
        return this.isInsideInsecureEnabled;
    }
    
    /** True if outside (secure) access is enabled. */
    public boolean getIsOutsideAccessEnabled()
    {
        return this.isOutsideAccessEnabled;
    }

    /** True if outside (secure) access is restricted. */
    public boolean getIsOutsideAccessRestricted()
    {
        return this.isOutsideAccessRestricted;
    }

    /**
     * The netmask of the network/host that is allowed to administer the box from outside
     * This is ignored if outside access is not enabled, null for just
     * one host.
     */

    /** The restricted network of machines allowed to connect to the box. */
    public IPaddr getOutsideNetwork()
    {
        return this.outsideNetwork;
    }
   
    /** The restricted netmask of machines allowed to connect to the box. */
    public IPaddr getOutsideNetmask()
    {
        return this.outsideNetmask;
    }

    /** --- HTTPs access configuration.  This shouldn't be here, --- **/
    public boolean getIsOutsideAdministrationEnabled()
    {
        return this.isOutsideAdministrationEnabled;
    }

    public boolean getIsOutsideQuarantineEnabled()
    {
        return this.isOutsideQuarantineEnabled;
    }
    
    public boolean getIsOutsideReportingEnabled()
    {
        return this.isOutsideReportingEnabled;
    }
        
    public AccessSettings toSettings()
    {
        AccessSettings settings = new AccessSettings();
        settings.setIsSshEnabled( getIsSshEnabled());
        settings.setIsInsideInsecureEnabled( getIsInsideInsecureEnabled());
        settings.setIsOutsideAccessEnabled( getIsOutsideAccessEnabled());
        settings.setIsOutsideAccessRestricted( getIsOutsideAccessRestricted());
        settings.setOutsideNetwork( getOutsideNetwork());
        settings.setOutsideNetmask( getOutsideNetmask());
        settings.setIsOutsideAdministrationEnabled( getIsOutsideAdministrationEnabled());
        settings.setIsOutsideQuarantineEnabled( getIsOutsideQuarantineEnabled());
        settings.setIsOutsideReportingEnabled( getIsOutsideReportingEnabled());
        return settings;
    }

    public static AccessSettingsInternal makeInstance( AccessSettings settings )
    {
        return new AccessSettingsInternal( settings );
    }
}