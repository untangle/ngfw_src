/**
 * $Id$
 */
package com.untangle.uvm;

import java.util.List;
import java.net.InetAddress;
import org.json.JSONArray;

import com.untangle.uvm.NetworkManager;

public interface NetworkManager
{

    /**
     * Remap the interfaces
     * @param osArray Array of os names (eth0, eth1, etc)
     * @param userArray Array of system names (External, Internal, etc);
     */
    void remapInterfaces( String[] osArray, String[] userArray ) throws Exception;

    /** Update the internal representation of the address */
    void refreshNetworkConfig();

    /* Get the hostname of the box */
    String getHostname();

    /* Forces the link status to be re-examined, since it is likely to
     * have changed */
    void updateLinkStatus();

    Boolean isQosEnabled();

    JSONArray getWANSettings();

    void setWANDownloadBandwidth(String name, int speed);

    void setWANUploadBandwidth(String name, int speed);
    
    void enableQos();

    /**
     * This returns an address where the host on the given interface should be able to access HTTP.
     # if HTTP is not reachable, this returns NULL
     */
    InetAddress getInternalHttpAddress( int clientIntf );

    String[] getPossibleInterfaces();

    String[] getWanInterfaces();

    boolean isWanInterface( int intfId );
    
}
