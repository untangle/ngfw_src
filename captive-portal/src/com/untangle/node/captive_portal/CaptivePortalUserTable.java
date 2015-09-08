/**
 * $Id$
 */

package com.untangle.node.captive_portal;

import java.net.InetAddress;
import java.util.Enumeration;
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
    private Hashtable<InetAddress, CaptivePortalUserEntry> userTable;

    public class StaleUser
    {
        CaptivePortalUserEvent.EventType reason;
        InetAddress address;

        StaleUser(InetAddress address, CaptivePortalUserEvent.EventType reason)
        {
            this.address = address;
            this.reason = reason;
        }
    }

    public CaptivePortalUserTable()
    {
        userTable = new Hashtable<InetAddress, CaptivePortalUserEntry>();
    }

    public ArrayList<CaptivePortalUserEntry> buildUserList()
    {
        ArrayList<CaptivePortalUserEntry> userList = new ArrayList<CaptivePortalUserEntry>(userTable.values());
        return (userList);
    }

    public CaptivePortalUserEntry insertActiveUser(InetAddress address, String username, Boolean anonymous)
    {
        CaptivePortalUserEntry local = new CaptivePortalUserEntry(address, username, anonymous);
        return insertActiveUser( local );
    }

    public CaptivePortalUserEntry insertActiveUser( CaptivePortalUserEntry local )
    {
        userTable.put(local.getUserAddress(), local);

        // For anonymous users clear the global capture username which
        // shouldn't be required but always better safe than sorry.  We
        // also set the captive portal flag to prevent the entry from being
        // timed-out while active in our table.
        if (local.getAnonymous() == true) {
            HostTableEntry entry = UvmContextFactory.context().hostTable().getHostTableEntry(local.getUserAddress(), true);
            entry.setUsernameCapture(null);
            entry.setCaptivePortalAuthenticated(true);
        }

        // for all other users set the global capture username and also
        // the captive portal flag so we don't get timed-out of the table
        else {
            HostTableEntry entry = UvmContextFactory.context().hostTable().getHostTableEntry(local.getUserAddress(), true);
            entry.setUsernameCapture(local.getUserName());
            entry.setCaptivePortalAuthenticated(true);
        }

        return (local);
    }

    public boolean removeActiveUser(InetAddress address)
    {
        CaptivePortalUserEntry user = userTable.get(address);
        if (user == null) return (false);
        userTable.remove(address);

        // clear the capture username from the host table entry and turn
        // of the captive portal flag so it knows we are all done 
        HostTableEntry entry = UvmContextFactory.context().hostTable().getHostTableEntry(address, true);
        entry.setUsernameCapture(null);
        entry.setCaptivePortalAuthenticated(false);
        return (true);
    }

    public CaptivePortalUserEntry searchByAddress(InetAddress address)
    {
        return (userTable.get(address));
    }

    public CaptivePortalUserEntry searchByUsername(String username, boolean ignoreCase)
    {
        Enumeration<CaptivePortalUserEntry> ee = userTable.elements();

        while (ee.hasMoreElements()) {
            CaptivePortalUserEntry item = ee.nextElement();

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
        long currentTime, idleTrigger, idleTrigger2, userTrigger;
        StaleUser stale;
        int wipecount;

        currentTime = (System.currentTimeMillis() / 1000);
        Enumeration<CaptivePortalUserEntry> ee = userTable.elements();
        wipecount = 0;

        while (ee.hasMoreElements()) {
            CaptivePortalUserEntry item = ee.nextElement();
            userTrigger = ((item.getSessionCreation() / 1000) + userTimeout);

            HostTableEntry entry = UvmContextFactory.context().hostTable().getHostTableEntry(item.getUserAddress());
            if (entry != null) {
                idleTrigger = (entry.getLastSessionTime() / 1000) + idleTimeout;
            } else {
                logger.warn("HostTableEntry missing for logged in Captive Portal Entry: " + item.getUserAddress().getHostAddress() + " : " + item.getUserName());
                idleTrigger = ((item.getSessionActivity() / 1000) + userTimeout);
            }

            // look for users with no traffic within the configured non-zero
            // idle timeout
            if ((idleTimeout > 0) && (currentTime > idleTrigger)) {
                logger.info("Idle timeout removing user " + item.getUserAddress() + " " + item.getUserName());
                stale = new StaleUser(item.getUserAddress(), CaptivePortalUserEvent.EventType.INACTIVE);
                wipelist.add(stale);
                wipecount++;
            }

            // look for users who have exceeded the configured maximum session
            // time
            if (currentTime > userTrigger) {
                logger.info("Session timeout removing user " + item.getUserAddress() + " " + item.getUserName());
                stale = new StaleUser(item.getUserAddress(), CaptivePortalUserEvent.EventType.TIMEOUT);
                wipelist.add(stale);
                wipecount++;
            }
        }

        return (wipelist);
    }

    public void purgeAllUsers()
    {
        userTable.clear();
    }
}
