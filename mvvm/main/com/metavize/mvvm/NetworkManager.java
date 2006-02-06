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

import com.metavize.mvvm.networking.NetworkSettings;
import com.metavize.mvvm.networking.NetworkException;
import com.metavize.mvvm.networking.BasicNetworkSettings;

public interface NetworkManager
{
    /**
     * Retrieve the basic network settings
     */
    public BasicNetworkSettings getBasicNetworkSettings();

    /* Save the basic network settings */
    public void setNetworkSettings( BasicNetworkSettings configuration ) 
        throws NetworkException, ValidateException;

    /**
     * Retrieve the current network configuration
     */
    public NetworkSettings getNetworkSettings();
    
    /**
     * Set a network configuration.
     * @param configuration - Configuration to save
     */
    public void setNetworkSettings( NetworkSettings networkSettings ) 
        throws NetworkException, ValidateException;
    
    /* Renew the DHCP address and return a new network settings with the updated address */
    public NetworkSettings renewDhcpLease() throws Exception;

    /* Retrieve a mapping of all of the interfaces */
    public IntfEnum getIntfEnum();

    /* Get the external HTTPS port */
    public int getExternalHttpsPort();

    /* Get the hostname of the box */
    public String getHostname();

    /* Get the public URL of the box */
    public String getPublicAddress();
}
