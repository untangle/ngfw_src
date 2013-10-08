/**
 * $Id: NetworkSettingsListener.java 34014 2013-02-25 22:48:02Z dmorris $
 */
package com.untangle.uvm.network;

import com.untangle.uvm.network.NetworkSettings;

/**
 * An API for listeners to the network settings
 * All subscribed network listeners will be called when the NetworkSettings changes
 */
public interface NetworkSettingsListener
{
    public void event( NetworkSettings settings );
}
