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

package com.untangle.uvm.networking;

import java.net.InetAddress;

import com.untangle.uvm.NetworkManager;
import com.untangle.uvm.node.IPSessionDesc;

import com.untangle.uvm.networking.internal.AccessSettingsInternal;
import com.untangle.uvm.networking.internal.AddressSettingsInternal;
import com.untangle.uvm.networking.internal.MiscSettingsInternal;
import com.untangle.uvm.networking.internal.NetworkSpacesInternalSettings;
import com.untangle.uvm.networking.internal.ServicesInternalSettings;
import com.untangle.uvm.node.ValidateException;

public interface LocalNetworkManager extends NetworkManager
{
    public NetworkSpacesInternalSettings getNetworkInternalSettings();

    public ServicesInternalSettings getServicesInternalSettings();

    public AccessSettingsInternal getAccessSettingsInternal();
    public AddressSettingsInternal getAddressSettingsInternal();
    public MiscSettingsInternal getMiscSettingsInternal();

    /* Register a service that needs outside access to HTTPs, the name should be unique */
    public void registerService( String name );

    /* Remove a service that needs outside access to HTTPs, the name should be unique */
    public void unregisterService( String name );

    public void setNetworkSettings( NetworkSpacesSettings settings, boolean configure )
        throws NetworkException, ValidateException;

    public void setServicesSettings( ServicesSettings servicesSettings )
        throws NetworkException;

    public void setServicesSettings( DhcpServerSettings dhcp, DnsServerSettings dns )
        throws NetworkException;
    
    /* This returns an address where the host should be able to access
     * HTTP.  if HTTP is not reachable, this returns NULL */
    public InetAddress getInternalHttpAddress( IPSessionDesc session );

    /* Insert all of the dynamic leases with their current values */
    public void updateLeases( DhcpServerSettings settings );

    public void startServices() throws NetworkException;

    public void stopServices();

    public void disableNetworkSpaces() throws NetworkException;

    public void enableNetworkSpaces() throws NetworkException;

    public void registerListener( NetworkSettingsListener networkListener );

    public void unregisterListener( NetworkSettingsListener networkListener );

    public void registerListener( AddressSettingsListener remoteListener );

    public void unregisterListener( AddressSettingsListener remoteListener );

    public void registerListener( IntfEnumListener intfEnumListener );

    public void unregisterListener( IntfEnumListener intfEnumListener );
}