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

public interface NetworkingManager  {
    /**
     * Retrieve the current network configuration
     */
    NetworkingConfiguration get();
    
    /**
     * Set a network configuration.
     * @param configuration - Configuration to save
     */
    void set( NetworkingConfiguration configuration ) throws ValidateException;
    
    NetworkingConfiguration renewDhcpLease() throws Exception;

    IntfEnum getIntfEnum();

    /* Get the external HTTPS port */
    public int getExternalHttpsPort();
}
