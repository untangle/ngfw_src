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

package com.untangle.mvvm.networking;

import com.untangle.mvvm.NetworkManager;
import com.untangle.mvvm.NetworkingConfiguration;
import com.untangle.mvvm.tran.HostName;
import com.untangle.mvvm.tran.IPaddr;
import com.untangle.mvvm.tran.ValidateException;

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
    public NetworkingConfiguration getNetworkingConfiguration()
    {
        return lnm.getNetworkingConfiguration();
    }

    /* Save the basic network settings */
    public void setNetworkingConfiguration( NetworkingConfiguration configuration )
        throws NetworkException, ValidateException
    {
        lnm.setNetworkingConfiguration( configuration );
    }

    /* Save the basic network settings during the wizard */
    public void setSetupNetworkingConfiguration( NetworkingConfiguration configuration )
        throws NetworkException, ValidateException
    {
        lnm.setSetupNetworkingConfiguration( configuration );
    }

    /* Use this to retrieve just the remote settings */
    public RemoteSettings getRemoteSettings()
    {
        return lnm.getRemoteSettings();
    }

    /* Use this to mess with the remote settings without modifying the network settings */
    public void setRemoteSettings( RemoteSettings remote ) throws NetworkException
    {
        lnm.setRemoteSettings( remote );
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
    public NetworkingConfiguration renewDhcpLease() throws NetworkException
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