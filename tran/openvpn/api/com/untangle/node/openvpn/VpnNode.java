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

package com.untangle.tran.openvpn;

import com.untangle.mvvm.tran.HostAddress;
import com.untangle.mvvm.tran.IPaddr;
import com.untangle.mvvm.tran.Transform;

import com.untangle.mvvm.tran.TransformException;
import com.untangle.mvvm.tran.ValidateException;

import com.untangle.mvvm.logging.EventManager;


import java.util.List;

public interface VpnTransform extends Transform
{
    public void setVpnSettings( VpnSettings settings );
    public VpnSettings getVpnSettings();

    /* Create a client certificate, if the client already has a certificate
     * this will automatically revoke their old one */
    public VpnClientBase generateClientCertificate( VpnSettings settings, VpnClientBase client );

    /* Revoke a client license */
    public VpnClientBase revokeClientCertificate( VpnSettings settings, VpnClientBase client );

    /* Need the address to log where the request came from */
    public String lookupClientDistributionKey( String key, IPaddr address );

    /* Send out the client distribution */
    public void distributeClientConfig( VpnClientBase client ) throws TransformException;

    public enum ConfigState { UNCONFIGURED, CLIENT, SERVER_BRIDGE, SERVER_ROUTE }
    public ConfigState getConfigState();
    public HostAddress getVpnServerAddress();

    public void startConfig(ConfigState state) throws ValidateException;
    public void completeConfig() throws Exception;

    //// the stages of the setup wizard ///
    List<String> getAvailableUsbList() throws TransformException;
    public void downloadConfig( HostAddress address, int port, String key ) throws Exception;
    public void downloadConfigUsb( String name ) throws Exception;
    public void generateCertificate( CertificateParameters parameters ) throws Exception;
    public GroupList getAddressGroups() throws Exception;
    public void setAddressGroups( GroupList parameters ) throws Exception;
    public void setExportedAddressList( ExportList parameters ) throws Exception;
    public void setClients( ClientList parameters ) throws Exception;
    public void setSites( SiteList parameters ) throws Exception;

    /**
     * Access the EventManager for ClientConnectEvents
     */
    public EventManager<ClientConnectEvent> getClientConnectEventManager();
    /**
     * Access the EventManager for VpnStatisticEvents
     */    
    public EventManager<VpnStatisticEvent> getVpnStatisticEventManager();
    /**
     * Access the EventManager for ClientDistributionEvents
     */    
    public EventManager<ClientDistributionEvent> getClientDistributionEventManager();
}
