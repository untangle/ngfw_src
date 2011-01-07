/* $HeadURL$ */
package com.untangle.uvm.networking;

public interface LocalMiscManager
{
    /* Use this to retrieve just the remote settings */
    public MiscSettings getSettings();
    
    /* Use this to mess with the remote settings without modifying the network settings */
    public void setSettings( MiscSettings settings );
}
