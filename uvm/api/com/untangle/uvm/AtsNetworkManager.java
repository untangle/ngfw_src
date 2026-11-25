package com.untangle.uvm;

import java.net.InetAddress;

import com.untangle.uvm.network.NetworkSettings;

/**
 * Combined Safe and additional ATS operations.
 */
public interface AtsNetworkManager extends SafeNetworkManager
{
    /**
     * getFirstWanAddress through the additional ATS contract.
     * @return The delegated result
     */
    InetAddress getFirstWanAddress();

    /**
     * getFullyQualifiedHostname through the additional ATS contract.
     * @return The delegated result
     */
    String getFullyQualifiedHostname();

    /**
     * getInterfaceHttpAddress through the additional ATS contract.
     * @param clientIntf The clientIntf argument
     * @return The delegated result
     */
    InetAddress getInterfaceHttpAddress(int clientIntf);

    /**
     * getNextFreeInterfaceId through the additional ATS contract.
     * @param netSettings The netSettings argument
     * @return The delegated result
     */
    int getNextFreeInterfaceId(NetworkSettings netSettings);
}
