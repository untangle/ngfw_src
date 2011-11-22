/* $HeadURL$ */
package com.untangle.uvm.networking;

public interface LocalAccessManager
{
    /* Use this to retrieve just the remote settings */
    public AccessSettings getSettings();
    
    /* Use this to mess with the remote settings without modifying the network settings */
    public void setSettings( AccessSettings settings );
}
