/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.openvpn;

import com.metavize.mvvm.tran.Transform;
import com.metavize.mvvm.tran.IPaddr;

import com.metavize.mvvm.tran.TransformException;
import com.metavize.mvvm.tran.ValidateException;

import com.metavize.mvvm.logging.EventManager;


import java.util.List;

public interface VpnTransform extends Transform
{
    public void setVpnSettings( VpnSettings settings );
    public VpnSettings getVpnSettings();

    /* Create a client certificate, if the client already has a certificate
     * this will automatically revoke their old one */
    public VpnClient generateClientCertificate( VpnSettings settings, VpnClient client );

    /* Revoke a client license */
    public VpnClient revokeClientCertificate( VpnSettings settings, VpnClient client );

    /* Need the address to log where the request came from */
    public String lookupClientDistributionKey( String key, IPaddr address );

    /* Send out the client distribution */
    public void distributeClientConfig( VpnClient client ) throws TransformException;

    public enum ConfigState { UNCONFIGURED, CLIENT, SERVER_BRIDGE, SERVER_ROUTE }
    public ConfigState getConfigState();
    public IPaddr getVpnServerAddress();

    public void startConfig(ConfigState state) throws ValidateException;
    public void completeConfig() throws Exception;

    //// the stages of the setup wizard ///
    List<String> getAvailableUsbList() throws TransformException;
    public void downloadConfig( IPaddr address, int port, String key ) throws Exception;
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
