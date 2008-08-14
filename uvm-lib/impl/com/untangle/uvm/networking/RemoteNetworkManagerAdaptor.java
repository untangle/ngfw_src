/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.networking;

import java.util.List;

import com.untangle.uvm.RemoteNetworkManager;
import com.untangle.uvm.node.HostName;
import com.untangle.uvm.node.IPaddr;
import com.untangle.uvm.node.ValidateException;

public class RemoteNetworkManagerAdaptor implements RemoteNetworkManager
{
    final LocalNetworkManager lnm;

    public RemoteNetworkManagerAdaptor(LocalNetworkManager lnm)
    {
        this.lnm = lnm;
    }

    /**
     * Retrieve the basic network settings
     */
    public BasicNetworkSettings getBasicSettings()
    {
        return lnm.getBasicSettings();
    }

    /* Save the basic network settings during the wizard */
    public synchronized void setSetupSettings( AddressSettings address, BasicNetworkSettings basic )
        throws NetworkException, ValidateException
    {
        this.lnm.setSetupSettings( address, basic );
    }

    public BasicNetworkSettings setSetupSettings( BasicNetworkSettings basic )
        throws NetworkException, ValidateException
    {
        return this.lnm.setSetupSettings( basic );
    }

    /* Set the access and address settings, used by the Remote Panel */
    public void setSettings( AccessSettings access, AddressSettings address )
        throws NetworkException, ValidateException
    {
        this.lnm.setSettings( access, address );
    }

    /* Set the Access, Misc and Network settings at once.  Used by the
     * support panel */
    public void setSettings( AccessSettings access, MiscSettings misc )
        throws NetworkException, ValidateException
    {
        this.lnm.setSettings( access, misc );
    }

    /**
     * Retrieve the settings related to limiting access to the box.
     */
    public AccessSettings getAccessSettings()
    {
        return this.lnm.getAccessSettings();
    }

    public void setAccessSettings( AccessSettings access )
    {
        this.lnm.setAccessSettings( access );
    }

    /**
     * Retrieve the settings related to the hostname and the address used to access to the box.
     */
    public AddressSettings getAddressSettings()
    {
        return this.lnm.getAddressSettings();
    }

    public void setAddressSettings( AddressSettings address )
    {
        this.lnm.setAddressSettings( address );
    }

    /**
     * Retrieve the miscellaneous settings that don't really belong anywhere else.
     */
    public MiscSettings getMiscSettings()
    {
        return lnm.getMiscSettings();
    }

    public void setMiscSettings( MiscSettings misc )
    {
        lnm.setMiscSettings( misc );
    }

    /**
     * Retrieve the current network configuration
     */
    public NetworkSpacesSettingsImpl getNetworkSettings()
    {
        return lnm.getNetworkSettings();
    }

    public List<Interface> getInterfaceList( boolean updateStatus )
    {
        return lnm.getInterfaceList( updateStatus );
    }

    public void remapInterfaces( String[] osArray, String[] userArray ) throws NetworkException
    {
        lnm.remapInterfaces( osArray, userArray );
    }
        
    /** Update the internal representation of the address */
    public void updateAddress() throws NetworkException
    {
        lnm.updateAddress();
    }

    /* Get the external HTTPS port */
    public int getPublicHttpsPort()
    {
        return lnm.getPublicHttpsPort();
    }

    /* Get the hostname of the box */
    public HostName getHostname()
    {
        return lnm.getHostname();
    }

    /* Get the public URL of the box */
    public String getPublicAddress()
    {
        return lnm.getPublicAddress();
    }

    /* Allow the setup wizard to setup NAT properly, or disable it. */
    public void setWizardNatEnabled( IPaddr address, IPaddr netmask )
    {
        lnm.setWizardNatEnabled( address, netmask );
    }

    public void setWizardNatDisabled()
    {
        lnm.setWizardNatDisabled();
    }

    /* Returns true if address is local to the edgeguard */
    public boolean isAddressLocal( IPaddr address )
    {
        return lnm.isAddressLocal( address );
    }

    /* Returns true if single nic mode is enabled */
    public boolean isSingleNicModeEnabled()
    {
        return lnm.isSingleNicModeEnabled();
    }

    /* Forces the link status to be re-examined, since it is likely to have changed */
    public void updateLinkStatus()
    {
        lnm.updateLinkStatus();
    }
}
