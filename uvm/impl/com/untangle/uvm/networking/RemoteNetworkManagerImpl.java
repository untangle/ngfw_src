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

import com.untangle.uvm.NetworkManager;
import com.untangle.uvm.node.HostName;
import com.untangle.uvm.node.IPaddr;
import com.untangle.uvm.node.ValidateException;

public class RemoteNetworkManagerImpl implements NetworkManager
{
    final LocalNetworkManager lnm;

    public RemoteNetworkManagerImpl( LocalNetworkManager lnm )
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

    /* Save the basic network settings */
    public void setBasicSettings( BasicNetworkSettings basic )
        throws NetworkException, ValidateException
    {
        this.lnm.setBasicSettings( basic );
    }

    /* Save the basic network settings during the wizard */
    public synchronized void setSetupSettings( AddressSettings address, BasicNetworkSettings basic ) 
        throws NetworkException, ValidateException
    {
        this.lnm.setSetupSettings( address, basic );
    }

    /* Set the network settings and the address settings at once, used
     * by the networking panel */
    public void setSettings( BasicNetworkSettings basic, AddressSettings address )
        throws NetworkException, ValidateException
    {
        this.lnm.setSettings( basic, address );
    }

    /* Set the access and address settings, used by the Remote Panel */
    public void setSettings( AccessSettings access, AddressSettings address )
        throws NetworkException, ValidateException
    {
        this.lnm.setSettings( access, address );
    }

    /* Set the Access, Misc and Network settings at once.  Used by the
     * support panel */
    public void setSettings( AccessSettings access, MiscSettings misc, NetworkSpacesSettings network )
        throws NetworkException, ValidateException
    {
        this.lnm.setSettings( access, misc, network );
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

    /**
     * Set a network configuration.
     * @param configuration - Configuration to save
     */
    public void setNetworkSettings( NetworkSpacesSettings networkSettings )
        throws NetworkException, ValidateException
    {
        lnm.setNetworkSettings( networkSettings );
    }

    /** Update the internal representation of the address */
    public void updateAddress() throws NetworkException
    {
        lnm.updateAddress();
    }

    public void pppoe( String args[] ) throws NetworkException
    {
        lnm.pppoe( args );
    }

    /* Get the current dynamic dns settings */
    public DynamicDNSSettings getDynamicDnsSettings()
    {
        return lnm.getDynamicDnsSettings();
    }

    /* Set the dynamic dns settings */
    public void setDynamicDnsSettings( DynamicDNSSettings newValue )
    {
        lnm.setDynamicDnsSettings( newValue );
    }

    /** This should require an interface list to block */
    public void disableDhcpForwarding()
    {
        lnm.disableDhcpForwarding();
    }

    public void enableDhcpForwarding()
    {
        lnm.enableDhcpForwarding();
    }

    public void subscribeLocalOutside( boolean newValue )
    {
        lnm.subscribeLocalOutside( newValue );
    }

    /* Retrieve a mapping of all of the interfaces, this presently lives in the
     * networking manager*/
    // public IntfEnum getIntfEnum();

    /* Renew the DHCP address and return a new network settings with the updated address */
    public BasicNetworkSettings renewDhcpLease() throws NetworkException
    {
        return lnm.renewDhcpLease();
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

    /* Forces the link status to be re-examined, since it is likely to have changed */
    public void updateLinkStatus()
    {
        lnm.updateLinkStatus();
    }
}