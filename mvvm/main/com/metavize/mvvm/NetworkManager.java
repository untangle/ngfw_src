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

import com.metavize.mvvm.tran.ValidateException;

import com.metavize.mvvm.networking.NetworkException;
import com.metavize.mvvm.networking.BasicNetworkSettings;
import com.metavize.mvvm.networking.NetworkSpacesSettings;
import com.metavize.mvvm.networking.DhcpStatus;

public interface NetworkManager
{
    /**
     * Retrieve the basic network settings
     */
    public NetworkingConfiguration getNetworkingConfiguration();

    /* Save the basic network settings */
    public void setNetworkingConfiguration( NetworkingConfiguration configuration ) 
        throws NetworkException, ValidateException;

    /**
     * Retrieve the current network configuration
     */
    public NetworkSpacesSettings getNetworkSettings();
    
    /**
     * Set a network configuration.
     * @param configuration - Configuration to save
     */
    public void setNetworkSettings( NetworkSpacesSettings networkSettings ) 
        throws NetworkException, ValidateException;

    /** Update the internal representation of the address */
    public void updateAddress() throws NetworkException;

    /** This should require an interface list to block */
    public void disableDhcpForwarding();
    public void enableDhcpForwarding();

    public void subscribeLocalOutside( boolean newValue );
    
    /* Retrieve a mapping of all of the interfaces */
    public IntfEnum getIntfEnum();

    /* Renew the DHCP address and return a new network settings with the updated address */
    public NetworkingConfiguration renewDhcpLease() throws NetworkException;
    
    /* Renew the DHCP address for a network space. */
    public DhcpStatus renewDhcpLease( int index ) throws NetworkException;

    /* Get the external HTTPS port */
    public int getPublicHttpsPort();

    /* Get the hostname of the box */
    public String getHostname();

    /* Get the public URL of the box */
    public String getPublicAddress();
}
