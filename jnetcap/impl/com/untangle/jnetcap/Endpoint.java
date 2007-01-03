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

package com.untangle.jnetcap;

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
}
