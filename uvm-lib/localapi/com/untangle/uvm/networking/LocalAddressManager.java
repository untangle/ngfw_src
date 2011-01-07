/* $HeadURL$ */
package com.untangle.uvm.networking;

public interface LocalAddressManager
{
    /* Use this to retrieve just the remote settings */
    public AddressSettings getSettings();
    
    /* Use this to mess with the remote settings without modifying the network settings */
    public void setSettings( AddressSettings settings );
}
