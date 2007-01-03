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

import java.net.InetAddress;

import com.untangle.mvvm.NetworkManager;
import com.untangle.mvvm.api.IPSessionDesc;
import com.untangle.mvvm.networking.internal.NetworkSpacesInternalSettings;
import com.untangle.mvvm.networking.internal.RemoteInternalSettings;
import com.untangle.mvvm.networking.internal.ServicesInternalSettings;
import com.untangle.mvvm.tran.ValidateException;

public interface LocalNetworkManager extends NetworkManager
{
    public RemoteInternalSettings getRemoteInternalSettings();

    public NetworkSpacesInternalSettings getNetworkInternalSettings();

    public ServicesInternalSettings getServicesInternalSettings();

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

    public void registerListener( RemoteSettingsListener remoteListener );

    public void unregisterListener( RemoteSettingsListener remoteListener );

    public void registerListener( IntfEnumListener intfEnumListener );

    public void unregisterListener( IntfEnumListener intfEnumListener );
}