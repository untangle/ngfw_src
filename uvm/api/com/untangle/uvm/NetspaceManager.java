/**
 * $Id: NetspaceManager.java 42115 2016-01-15 17:56:36Z mahotz $
 */

package com.untangle.uvm;

import java.net.InetAddress;
import com.untangle.uvm.app.IPMaskedAddress;

public interface NetspaceManager
{
    void registerNetworkBlock(String ownerName, String ownerPurpose, InetAddress networkAddress, Integer networkSize);

    void registerNetworkBlock(String ownerName, String ownerPurpose, String networkText);

    void registerNetworkBlock(String ownerName, String ownerPurpose, IPMaskedAddress networkInfo);

    void clearOwnerRegistrationAll(String ownerName);

    void clearOwnerRegistrationPurpose(String ownerName, String ownerPurpose);

    NetworkSpace isNetworkAvailable(String ownerName, InetAddress networkAddress, Integer networkSize);

    NetworkSpace isNetworkAvailable(String ownerName, String networkText);

    NetworkSpace isNetworkAvailable(String ownerName, IPMaskedAddress tester);

    IPMaskedAddress getAvailableAddressSpace();
    
    InetAddress getFirstUsableAddress(InetAddress networkAddress, Integer networkSize);

    InetAddress getFirstUsableAddress(String networkText);

    /**
     * Stores details about a network address block
     *
     */
    class NetworkSpace
    {
        public String ownerName;
        public String ownerPurpose;
        public IPMaskedAddress maskedAddress;

        /**
         * Gets a string representation for logging
         * 
         * @return The string representation
         */
        public String toString()
        {
            String string = new String();
            string = "OWNER:" + ownerName + " PURPOSE:" + ownerPurpose + " NETWORK:" + maskedAddress.toString();
            return string;
        }
    }
}
