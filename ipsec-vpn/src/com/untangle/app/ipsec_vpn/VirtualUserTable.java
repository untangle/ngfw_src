/**
 * $Id: VirtualUserTable.java 37267 2014-02-26 23:42:19Z dmorris $
 */

package com.untangle.app.ipsec_vpn;

import java.net.InetAddress;
import java.util.Hashtable;
import java.util.LinkedList;
import org.apache.log4j.Logger;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.HostTableEntry;

/**
 * This class implements the table used to track active IPsec VPN users who
 * connect using L2TP, Xauth, or IKEv2.
 * 
 * @author mahotz
 * 
 */

public class VirtualUserTable
{
    private final Logger logger = Logger.getLogger(getClass());
    private Hashtable<InetAddress, VirtualUserEntry> userTable;

    /**
     * Constructor
     */
    public VirtualUserTable()
    {
        userTable = new Hashtable<>();
    }

    /**
     * Creates a list of all active virtual users.
     * 
     * @return The list of active virtual users.
     */
    public LinkedList<VirtualUserEntry> buildUserList()
    {
        LinkedList<VirtualUserEntry> userList = new LinkedList<>(userTable.values());
        return (userList);
    }

    /**
     * Adds a user to the list of active virtual users.
     * 
     * @param clientProtocol
     *        - The protocol used to connect. (L2TP | XAUTH | IKEv2)
     * @param clientAddress
     *        - The IP address assigned to the client
     * @param clientUsername
     *        - The username of the client
     * @param netInterface
     *        - The interface identifier assigned to the client
     * @param netProcess
     *        - The process identifier used to control the connection
     * @return The VirtualUserEntry object inserted into the table
     */
    public VirtualUserEntry insertVirtualUser(String clientProtocol, InetAddress clientAddress, String clientUsername, String netInterface, String netProcess)
    {
        // set the global tunnel username and also the tunnel marker so we don't get timed-out of the table
        HostTableEntry entry = UvmContextFactory.context().hostTable().getHostTableEntry(clientAddress, true);
        entry.setUsernameIpsecVpn(clientUsername);

        // store the user details in our local table
        VirtualUserEntry local = new VirtualUserEntry(clientProtocol, clientAddress, clientUsername, netInterface, netProcess);
        userTable.put(clientAddress, local);

        return (local);
    }

    /**
     * Removes a user from the list of active virtual users. If allow concurrent
     * logins is TRUE, we also remove the IPsec username from the host table
     * entry to preserve legacy behavior. If the concurrent flag is FALSE, we
     * leave the name alone so it can be found when the same user logs in again.
     * This new logic was added to solve NGFW-11210 where the same user bounces
     * around and re-connects from different networks, filling the host table
     * with mulitple entries that consume available license count. Keeping the
     * IPsec username lets us find and re-use the previous host table entry.
     * 
     * TODO - Could there be negative side effects with leaving the username in
     * the host table after disconnect? Wrong user shows up in reports? Probably
     * not since the IP address should only be assigned to another IPsec user,
     * which would capture the new username, but mentioning just in case.
     * 
     * @param clientAddress
     *        The IP address of the client to be removed.
     * @param concurrentFlag
     *        The allow concurrent logins flag
     * @return True if found and removed, otherwise false.
     */
    public boolean removeVirtualUser(InetAddress clientAddress, boolean concurrentFlag)
    {
        if (concurrentFlag) {
            // clear the global tunnel username so host table knows we are done
            HostTableEntry entry = UvmContextFactory.context().hostTable().getHostTableEntry(clientAddress, false);
            if (entry != null) entry.setUsernameIpsecVpn(null);
        }

        // clear the user detail sfrom our local table
        VirtualUserEntry user = userTable.get(clientAddress);
        if (user == null) return (false);
        userTable.remove(clientAddress);
        return (true);
    }

    /**
     * Searches for a virtual user by address.
     * 
     * @param clientAddress
     *        - The address to search
     * @return The user entry for the argumented address, or NULL if not found.
     */
    public VirtualUserEntry searchVirtualUser(InetAddress clientAddress)
    {
        return (userTable.get(clientAddress));
    }

    /**
     * Returns the number of virtual users currently connected.
     * 
     * @return The number of virtual users currently connected.
     */
    public long countVirtualUsers()
    {
        return (userTable.size());
    }
}
