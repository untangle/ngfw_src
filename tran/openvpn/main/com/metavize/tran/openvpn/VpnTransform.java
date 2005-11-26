/*
 * Copyright (c) 2005 Metavize Inc.
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


import java.util.List;

public interface VpnTransform extends Transform
{
    public void setVpnSettings( VpnSettings settings );
    public VpnSettings getVpnSettings();

    /* Create a new set of base parameters, this invalidates all of the client keys */
    public VpnSettings generateBaseParameters( VpnSettings settings );

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

    public void startConfig(ConfigState state) throws ValidateException;
    public void completeConfig() throws Exception;

    //// the stages of the setup wizard ///
    public void downloadConfig( IPaddr address, String key ) throws Exception;
    public void generateCertificate( CertificateParameters parameters ) throws Exception;
    public void setAddressGroups( GroupList parameters ) throws Exception;
    public void setExportedAddressList( ExportList parameters ) throws Exception;
    public void setClients( ClientList parameters ) throws Exception;
    public void setSites( SiteList parameters ) throws Exception;    
}
