/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: Endpoint.java,v 1.4 2005/01/03 23:34:33 rbscott Exp $
 */

package com.metavize.jnetcap;

import java.net.InetAddress;

public interface Endpoint
{
    /**
     * Retrieve the host for this endpoint
     */
    public InetAddress host();

    /**
     * Retrieve the port for the endpoint.
     */
    public int port();

    /** 
     * Retrieve the name of an interface of an empty string if the interface is unknown.
     */
    public String interfaceName();

    /**
     * Retrieve a unique interface identifier, or 0 if the interface is unknown
     */
    public byte interfaceId();
}
