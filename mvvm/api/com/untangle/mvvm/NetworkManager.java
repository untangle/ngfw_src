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

package com.untangle.mvvm;

import com.untangle.mvvm.tran.HostName;
import com.untangle.mvvm.tran.IPaddr;

import com.untangle.mvvm.networking.BasicNetworkSettings;
import com.untangle.mvvm.networking.DhcpStatus;
import com.untangle.mvvm.networking.DynamicDNSSettings;
import com.untangle.mvvm.networking.NetworkException;
import com.untangle.mvvm.networking.NetworkSpacesSettings;
import com.untangle.mvvm.networking.NetworkSpacesSettingsImpl;
import com.untangle.mvvm.networking.RemoteSettings;

import com.untangle.mvvm.tran.ValidateException;


/* XXXXXXXXX This should be renamed to RemoteNetworkManager */
public interface NetworkManager
{
    /**
     * Retrieve the basic network settings
     */
    public NetworkingConfiguration getNetworkingConfiguration();

    /* Save the basic network settings */
    public void setNetworkingConfiguration( NetworkingConfiguration configuration ) 
        throws NetworkException, ValidateException;

    /* Save the basic network settings during the wizard */
    public void setSetupNetworkingConfiguration( NetworkingConfiguration configuration ) 
        throws NetworkException, ValidateException;

    /* Use this to retrieve just the remote settings */
    public RemoteSettings getRemoteSettings();

    /* Use this to mess with the remote settings without modifying the network settings */
    public void setRemoteSettings( RemoteSettings remote ) throws NetworkException;

    /**
     * Retrieve the current network configuration
     */
    public NetworkSpacesSettingsImpl getNetworkSettings();
    
    /**
     * Set a network configuration.
     * @param configuration - Configuration to save
     */
    public void setNetworkSettings( NetworkSpacesSettings networkSettings ) 
        throws NetworkException, ValidateException;

    /** Update the internal representation of the address */
    public void updateAddress() throws NetworkException;

    /* Helper function for running some pppoe stuff */
    public void pppoe( String args[] ) throws NetworkException;

    /* Get the current dynamic dns settings */
    public DynamicDNSSettings getDynamicDnsSettings();

    /* Set the dynamic dns settings */
    public void setDynamicDnsSettings( DynamicDNSSettings newValue );

    /** This should require an interface list to block */
    public void disableDhcpForwarding();
    public void enableDhcpForwarding();

    public void subscribeLocalOutside( boolean newValue );
    
    /* Renew the DHCP address and return a new network settings with the updated address */
    public NetworkingConfiguration renewDhcpLease() throws NetworkException;
    
    /* Get the external HTTPS port */
    public int getPublicHttpsPort();

    /* Get the hostname of the box */
    public HostName getHostname();

    /* Get the public URL of the box */
    public String getPublicAddress();

    /* Allow the setup wizard to setup NAT properly, or disable it. */
    public void setWizardNatEnabled(IPaddr address, IPaddr netmask);
    public void setWizardNatDisabled();

    /* Returns true if address is local to the edgeguard */
    public boolean isAddressLocal( IPaddr address );

    /* Forces the link status to be re-examined, since it is likely to have changed */
    public void updateLinkStatus();
}
