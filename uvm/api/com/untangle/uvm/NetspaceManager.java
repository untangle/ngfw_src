/**
 * $Id: NetspaceManager.java 42115 2016-01-15 17:56:36Z mahotz $
 */

package com.untangle.uvm;

import java.io.Serializable;
import java.net.InetAddress;
import com.untangle.uvm.app.IPMaskedAddress;
import org.json.JSONObject;
import org.json.JSONString;

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

    IPMaskedAddress getAvailableAddressSpace(IPVersion version, int hostId);
    
    InetAddress getFirstUsableAddress(InetAddress networkAddress, Integer networkSize);

    InetAddress getFirstUsableAddress(String networkText);

    public static enum IPVersion { IPv4, IPv6 };

    /**
     * Stores details about a network address block
     *
     */
    @SuppressWarnings("serial")
    class NetworkSpace implements Serializable, JSONString
    {
        public String ownerName;
        public String ownerPurpose;
        public IPMaskedAddress maskedAddress;

        public String getOwnerName() { return ownerName; }
        public String getOwnerPurpose() { return ownerPurpose; }
        public IPMaskedAddress getMaskedAddress() { return maskedAddress; }

        /**
         * Gets a string representation for logging
         * 
         * @return The string representation
         */
        public String toString()
        {
            return "OWNER:" + ownerName + " PURPOSE:" + ownerPurpose + " NETWORK:" + maskedAddress.toString();
        }

        public String toJSONString()
        {
            JSONObject jO = new JSONObject(this);
            return jO.toString();
        }
    }
}
