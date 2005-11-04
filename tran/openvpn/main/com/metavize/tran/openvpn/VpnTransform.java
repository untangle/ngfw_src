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
}
