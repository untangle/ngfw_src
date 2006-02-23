/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.mvvm;

import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.networking.BasicNetworkSettings;
import com.metavize.mvvm.networking.DhcpStatus;
import com.metavize.mvvm.networking.NetworkException;
import com.metavize.mvvm.networking.NetworkSpacesSettings;
import com.metavize.mvvm.networking.NetworkSpacesSettingsImpl;
import com.metavize.mvvm.networking.RemoteSettings;
import com.metavize.mvvm.networking.DynamicDNSSettings;

import com.metavize.mvvm.tran.ValidateException;


public interface NetworkManager
{
    /**
     * Retrieve the basic network settings
     */
    public NetworkingConfiguration getNetworkingConfiguration();

    /* Save the basic network settings */
    public void setNetworkingConfiguration( NetworkingConfiguration configuration ) 
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

    /* Get the current dynamic dns settings */
    public DynamicDNSSettings getDynamicDnsSettings();

    /* Set the dynamic dns settings */
    public void setDynamicDnsSettings( DynamicDNSSettings newValue );

    /** This should require an interface list to block */
    public void disableDhcpForwarding();
    public void enableDhcpForwarding();

    public void subscribeLocalOutside( boolean newValue );
    
    /* Retrieve a mapping of all of the interfaces, this presently lives in the
     * networking manager*/
    // public IntfEnum getIntfEnum();

    /* Renew the DHCP address and return a new network settings with the updated address */
    public NetworkingConfiguration renewDhcpLease() throws NetworkException;
    
    /* Get the external HTTPS port */
    public int getPublicHttpsPort();

    /* Get the hostname of the box */
    public String getHostname();

    /* Get the public URL of the box */
    public String getPublicAddress();

    /* Allow the setup wizard to setup NAT properly, or disable it. */
    public void setWizardNatEnabled(IPaddr address, IPaddr netmask);
    public void setWizardNatDisabled();
}
