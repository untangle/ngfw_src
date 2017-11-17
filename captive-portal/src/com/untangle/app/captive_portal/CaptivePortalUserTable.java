/**
 * $Id$
 */

package com.untangle.app.captive_portal;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.TimerTask;
import org.apache.log4j.Logger;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.HostTable;
import com.untangle.uvm.HostTableEntry;

public class CaptivePortalUserTable
{
    private final Logger logger = Logger.getLogger(getClass());
    private Hashtable<String, CaptivePortalUserEntry> activeUserTable = null;
    private CaptivePortalApp ownerApp = null;

    public class StaleUser
    {
        CaptivePortalUserEvent.EventType reason;
        InetAddress netaddr;

        StaleUser(InetAddress netaddr, CaptivePortalUserEvent.EventType reason)
        {
            this.netaddr = netaddr;
            this.reason = reason;
        }
    }

    public CaptivePortalUserTable(CaptivePortalApp ownerApp)
    {
        this.ownerApp = ownerApp;
        activeUserTable = new Hashtable<String, CaptivePortalUserEntry>();
    }

    public ArrayList<CaptivePortalUserEntry> buildUserList()
    {
        ArrayList<CaptivePortalUserEntry> userList = new ArrayList<CaptivePortalUserEntry>();
        userList.addAll(activeUserTable.values());
        return (userList);
    }

    public CaptivePortalUserEntry insertActiveUser(String useraddr, String username, Boolean anonymous)
    {
        CaptivePortalUserEntry userEntry = new CaptivePortalUserEntry(useraddr, username, anonymous);
        activeUserTable.put(useraddr, userEntry);
        logger.debug("INSERT USER: " + userEntry.toString());
        return (userEntry);
    }

    protected CaptivePortalUserEntry insertActiveUser(CaptivePortalUserEntry userEntry)
    {
        logger.debug("INSERT USER: " + userEntry.toString());
        activeUserTable.put(userEntry.getUserAddress(), userEntry);
        return (userEntry);
    }

    public boolean removeActiveUser(String useraddr)
    {
        // find and remove from the active user table
        CaptivePortalUserEntry user = searchByAddress(useraddr);
        if (user == null) return (false);

        logger.debug("REMOVE USER: " + user.toString());
        activeUserTable.remove(user.getUserAddress());
        return (true);
    }

    public CaptivePortalUserEntry searchByAddress(String netaddr)
    {
        CaptivePortalUserEntry item = activeUserTable.get(netaddr);
        return (item);
    }

    public CaptivePortalUserEntry searchByUsername(String username, boolean ignoreCase)
    {
        CaptivePortalUserEntry item;

        for (String address : activeUserTable.keySet()) {
            item = activeUserTable.get(address);

            // if the ignoreCase flag is set we compare both as lowercase
            if (ignoreCase == true) {
                if (username.toLowerCase().equals(item.getUserName().toLowerCase()) == true) return (item);
            }

            // ignoreCase flag is not set so do a direct comparison
            else {
                if (username.equals(item.getUserName()) == true) return (item);
            }
        }

        return (null);
    }

    public ArrayList<StaleUser> buildStaleList(long idleTimeout, long userTimeout)
    {
        ArrayList<StaleUser> wipelist = new ArrayList<StaleUser>();
        HostTableEntry entry = null;
        long currentTime = (System.currentTimeMillis() / 1000);
        long idleTrigger = 0;
        long userTrigger = 0;
        int wipecount = 0;
        StaleUser stale;

        for (String address : activeUserTable.keySet()) {
            CaptivePortalUserEntry item = activeUserTable.get(address);
            userTrigger = ((item.getSessionCreation() / 1000) + userTimeout);

            if (address.indexOf(':') >= 0) {
                // any semi-colon in the key indicate a MAC address                
                entry = UvmContextFactory.context().hostTable().findHostTableEntryByMacAddress(address);
            } else {
                // not a mac address so do normal lookup by IP address
                entry = UvmContextFactory.context().hostTable().getHostTableEntry(address);
            }

            if (entry != null) {
                idleTrigger = (entry.getLastSessionTime() / 1000) + idleTimeout;
            } else {
                logger.warn("HostTableEntry missing for logged in Captive Portal user: " + item.toString());
                idleTrigger = ((item.getSessionActivity() / 1000) + userTimeout);
            }

            // look for users with no traffic within the configured non-zero idle timeout
            if ((idleTimeout > 0) && (currentTime > idleTrigger)) {
                logger.info("Idle timeout removing user " + item.toString());
                stale = new StaleUser(entry.getAddress(), CaptivePortalUserEvent.EventType.INACTIVE);
                wipelist.add(stale);
                wipecount++;
            }

            // look for users who have exceeded the configured maximum session time
            if (currentTime > userTrigger) {
                logger.info("Session timeout removing user " + item.toString());
                stale = new StaleUser(entry.getAddress(), CaptivePortalUserEvent.EventType.TIMEOUT);
                wipelist.add(stale);
                wipecount++;
            }
        }

        return (wipelist);
    }

    public void purgeAllUsers()
    {
        activeUserTable.clear();
    }
}
