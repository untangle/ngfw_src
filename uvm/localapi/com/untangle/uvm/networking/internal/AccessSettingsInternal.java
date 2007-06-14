/*
 * $HeadURL:$
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

package com.untangle.uvm.networking.internal;

import com.untangle.uvm.networking.AccessSettings;

import com.untangle.uvm.node.HostName;
import com.untangle.uvm.node.IPaddr;

/** These are settings related to remote access to the untangle.
 *  These are related to controlling access to resources on
 * the local server. */
public class AccessSettingsInternal
{
    private final boolean isSupportEnabled;

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
        this.isSupportEnabled = settings.getIsSupportEnabled();
        this.isInsideInsecureEnabled = settings.getIsInsideInsecureEnabled();
        this.isOutsideAccessEnabled = settings.getIsOutsideAccessEnabled();
        this.isOutsideAccessRestricted = settings.getIsOutsideAccessRestricted();
        this.outsideNetwork = settings.getOutsideNetwork();
        this.outsideNetmask = settings.getOutsideNetmask();

        this.isOutsideAdministrationEnabled = settings.getIsOutsideAdministrationEnabled();
        this.isOutsideQuarantineEnabled = settings.getIsOutsideQuarantineEnabled();
        this.isOutsideReportingEnabled = settings.getIsOutsideReportingEnabled();
    }
    
    /* Get whether or not support is enabled. */
    public boolean getIsSupportEnabled()
    {
        return this.isSupportEnabled;
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
        settings.setIsSupportEnabled( getIsSupportEnabled());
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
