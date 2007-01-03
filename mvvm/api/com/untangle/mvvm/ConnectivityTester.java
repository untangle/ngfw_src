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

package com.untangle.mvvm;

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
