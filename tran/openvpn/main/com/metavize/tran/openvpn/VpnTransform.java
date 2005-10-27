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

package com.metavize.tran.vpn;

import com.metavize.mvvm.tran.Transform;

public interface VpnTransform extends Transform
{
    public void setVpnSettings( VpnSettings settings );
    public VpnSettings getVpnSettings();

    public VpnSettings generateBaseParameters( String serverName, boolean isCaKeyLocal, 
                                               int keySize, String country, String state,
                                               String city, String organization, String email );
}
