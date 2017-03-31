/**
 * $Id: VirtualUserTable.java 37267 2014-02-26 23:42:19Z dmorris $
 */

package com.untangle.app.ipsec_vpn;

import java.net.InetAddress;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import org.apache.log4j.Logger;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.HostTable;
import com.untangle.uvm.HostTableEntry;

public class VirtualUserTable
{
    private final Logger logger = Logger.getLogger(getClass());
    private Hashtable<InetAddress, VirtualUserEntry> userTable;

    public VirtualUserTable()
    {
        userTable = new Hashtable<InetAddress, VirtualUserEntry>();
    }

    public LinkedList<VirtualUserEntry> buildUserList()
    {
        LinkedList<VirtualUserEntry> userList = new LinkedList<VirtualUserEntry>(userTable.values());
        return (userList);
    }

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

    public boolean removeVirtualUser(InetAddress clientAddress)
    {
        // clear the global tunnel username and turn off the tunnel marker so host table knows we are done
        HostTableEntry entry = UvmContextFactory.context().hostTable().getHostTableEntry(clientAddress, false);
        if (entry != null) {
            entry.setUsernameIpsecVpn(null);
        }

        // clear the user detail sfrom our local table
        VirtualUserEntry user = userTable.get(clientAddress);
        if (user == null) return (false);
        userTable.remove(clientAddress);
        return (true);
    }

    public VirtualUserEntry searchVirtualUser(InetAddress clientAddress)
    {
        return (userTable.get(clientAddress));
    }

    public long countVirtualUsers()
    {
        return (userTable.size());
    }
}
