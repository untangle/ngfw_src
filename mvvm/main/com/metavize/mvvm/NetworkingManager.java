/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: NetworkingManager.java,v 1.1 2005/02/23 03:51:48 rbscott Exp $
 */

package com.metavize.mvvm;

public interface NetworkingManager  {
    /**
     * Retrieve the current network configuration
     */
    NetworkingConfiguration get();
    
    /**
     * Set a network configuration.
     * @param configuration - Configuration to save
     */
    void set( NetworkingConfiguration configuration );
}
