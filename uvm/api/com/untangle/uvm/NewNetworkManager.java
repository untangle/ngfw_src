/**
 * $Id: NewNetworkManager.java,v 1.00 2013/01/07 12:15:14 dmorris Exp $
 */
package com.untangle.uvm;

import com.untangle.uvm.network.NetworkSettings;

public interface NewNetworkManager
{
    /**
     * Get the network settings
     */
    NetworkSettings getNetworkSettings();

    /**
     * Set the network settings
     */
    void setNetworkSettings( NetworkSettings newSettings );
}
