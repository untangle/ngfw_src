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

package com.metavize.mvvm;

public interface ConnectivityTester {
    /**
     * Retrieve the connectivity status of the network.
     */
    Status getStatus();

    interface Status
    {
        boolean isDnsWorking();
        
        boolean isTcpWorking();
    }
}
