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
    private Hashtable<InetAddress, CaptivePortalUserEntry> activeUserTable = null;
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
        activeUserTable = new Hashtable<InetAddress, CaptivePortalUserEntry>();
    }

    public ArrayList<CaptivePortalUserEntry> buildUserList()
    {
        ArrayList<CaptivePortalUserEntry> userList = new ArrayList<CaptivePortalUserEntry>();
        userList.addAll(activeUserTable.values());
        return (userList);
    }

    public CaptivePortalUserEntry insertActiveUser(InetAddress netaddr, String username, Boolean anonymous)
    {
        String macaddr = null;

        // do not pass the create flag here since it is passed in object insert call
        HostTableEntry entry = UvmContextFactory.context().hostTable().getHostTableEntry(netaddr);
        if (entry != null) macaddr = entry.getMacAddress();

        CaptivePortalUserEntry local = new CaptivePortalUserEntry(netaddr, macaddr, username, anonymous);
        return insertActiveUser(local);
    }

    protected CaptivePortalUserEntry insertActiveUser(CaptivePortalUserEntry local)
    {
        logger.debug("INSERT USER: " + local.toString());

        // set the mac login flag appropriately  
        if ((ownerApp.getSettings().getUseMacAddress()) && (local.getUserMacAddress() != null)) {
            local.setMacLogin(Boolean.TRUE);
        } else {
            local.setMacLogin(Boolean.FALSE);
        }

        activeUserTable.put(local.getUserNetAddress(), local);

        // For anonymous users clear the global capture username which
        // shouldn't be required but always better safe than sorry.  We
        // also set the captive portal flag to prevent the entry from being
        // timed-out while active in our table.
        if (local.getAnonymous() == true) {
            HostTableEntry entry = UvmContextFactory.context().hostTable().getHostTableEntry(local.getUserNetAddress(), true);
            if (entry != null) {
                entry.setUsernameCaptivePortal(null);
                entry.setCaptivePortalAuthenticated(true);
            }
        }

        // for all other users set the global capture username and also
        // the captive portal flag so we don't get timed-out of the table
        else {
            HostTableEntry entry = UvmContextFactory.context().hostTable().getHostTableEntry(local.getUserNetAddress(), true);
            if (entry != null) {
                entry.setUsernameCaptivePortal(local.getUserName());
                entry.setCaptivePortalAuthenticated(true);
            }
        }

        return (local);
    }

    public boolean removeActiveNetUser(InetAddress netaddr)
    {
        // find and remove from the active user table
        CaptivePortalUserEntry user = searchByNetAddress(netaddr);

        if (user != null) {
            logger.debug("REMOVE NET USER: " + user.toString());
            activeUserTable.remove(user.getUserNetAddress());
        }

        // get the host table entry for the argumented address
        HostTableEntry entry = UvmContextFactory.context().hostTable().getHostTableEntry(netaddr);

        // clear the capture username from the host table entry and turn
        // of the captive portal flag so it knows we are all done
        if (entry != null) {
            entry.setUsernameCaptivePortal(null);
            entry.setCaptivePortalAuthenticated(false);
        }

        // return status of our search for the user in our table 
        return (user == null ? false : true);
    }

    public boolean removeActiveMacUser(String macaddr)
    {
        // find and remove from the active user table
        CaptivePortalUserEntry user = searchByMacAddress(macaddr);

        if (user != null) {
            logger.debug("REMOVE MAC USER: " + user.toString());
            activeUserTable.remove(user.getUserNetAddress());
        }

        if (user == null) return (false);

        // clear the capture username from the host table entry and turn
        // of the captive portal flag so it knows we are all done
        HostTableEntry entry = UvmContextFactory.context().hostTable().findHostTableEntryByMacAddress(macaddr);
        if (entry != null) {
            entry.setUsernameCaptivePortal(null);
            entry.setCaptivePortalAuthenticated(false);
        }

        return (true);
    }

    public CaptivePortalUserEntry searchByNetAddress(InetAddress netaddr)
    {
        CaptivePortalUserEntry item = activeUserTable.get(netaddr);
        return (item);
    }

    public CaptivePortalUserEntry searchByMacAddress(String macaddr)
    {
        CaptivePortalUserEntry item;

        for (InetAddress address : activeUserTable.keySet()) {
            item = activeUserTable.get(address);
            if (item.getMacLogin() != Boolean.TRUE) continue; 
            if (macaddr.equals(item.getUserMacAddress())) return (item);
        }

        return (null);
    }

    public CaptivePortalUserEntry searchByUsername(String username, boolean ignoreCase)
    {
        CaptivePortalUserEntry item;

        for (InetAddress address : activeUserTable.keySet()) {
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
        long currentTime, idleTrigger, userTrigger;
        int wipecount = 0;
        StaleUser stale;

        currentTime = (System.currentTimeMillis() / 1000);

        for (InetAddress address : activeUserTable.keySet()) {
            CaptivePortalUserEntry item = activeUserTable.get(address);
            userTrigger = ((item.getSessionCreation() / 1000) + userTimeout);

            HostTableEntry entry = UvmContextFactory.context().hostTable().getHostTableEntry(item.getUserNetAddress());
            if (entry != null) {
                idleTrigger = (entry.getLastSessionTime() / 1000) + idleTimeout;
            } else {
                logger.warn("HostTableEntry missing for logged in Captive Portal Entry: " + item.getUserNetAddress().getHostAddress().toString() + " : " + item.getUserName());
                idleTrigger = ((item.getSessionActivity() / 1000) + userTimeout);
            }

            // look for users with no traffic within the configured non-zero idle timeout
            if ((idleTimeout > 0) && (currentTime > idleTrigger)) {
                logger.info("Idle timeout removing user " + item.getUserNetAddress() + " " + item.getUserName());
                stale = new StaleUser(item.getUserNetAddress(), CaptivePortalUserEvent.EventType.INACTIVE);
                wipelist.add(stale);
                wipecount++;
            }

            // look for users who have exceeded the configured maximum session time
            if (currentTime > userTrigger) {
                logger.info("Session timeout removing user " + item.getUserNetAddress() + " " + item.getUserName());
                stale = new StaleUser(item.getUserNetAddress(), CaptivePortalUserEvent.EventType.TIMEOUT);
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
