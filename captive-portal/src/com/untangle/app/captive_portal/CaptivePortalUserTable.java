/**
 * $Id$
 */

package com.untangle.app.captive_portal;

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
    private Hashtable<InetAddress, CaptivePortalUserEntry> netAddrTable = null;
    private Hashtable<String, CaptivePortalUserEntry> macAddrTable = null;
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

        macAddrTable = new Hashtable<String, CaptivePortalUserEntry>();
        netAddrTable = new Hashtable<InetAddress, CaptivePortalUserEntry>();
    }

    public ArrayList<CaptivePortalUserEntry> buildUserList()
    {
        ArrayList<CaptivePortalUserEntry> userList = new ArrayList<CaptivePortalUserEntry>();

        // if the mac table is active add all those values first
        if (ownerApp.getSettings().getUseMacAddress()) {
            userList.addAll(macAddrTable.values());
        }

        // next we add all the values from the net table
        userList.addAll(netAddrTable.values());

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
        if ((ownerApp.getSettings().getUseMacAddress()) && (local.getUserMacAddress() != null)) {
            logger.debug("INSERT MAC TABLE: " + local.toString());
            local.setMacLogin(Boolean.TRUE);
            macAddrTable.put(local.getUserMacAddress(), local);
        } else {
            logger.debug("INSERT NET TABLE: " + local.toString());
            local.setMacLogin(Boolean.FALSE);
            netAddrTable.put(local.getUserNetAddress(), local);
        }

        // For anonymous users clear the global capture username which
        // shouldn't be required but always better safe than sorry.  We
        // also set the captive portal flag to prevent the entry from being
        // timed-out while active in our table.
        if (local.getAnonymous() == true) {
            HostTableEntry entry = UvmContextFactory.context().hostTable().getHostTableEntry(local.getUserNetAddress(), true);
            entry.setUsernameCapture(null);
            entry.setCaptivePortalAuthenticated(true);
        }

        // for all other users set the global capture username and also
        // the captive portal flag so we don't get timed-out of the table
        else {
            HostTableEntry entry = UvmContextFactory.context().hostTable().getHostTableEntry(local.getUserNetAddress(), true);
            entry.setUsernameCapture(local.getUserName());
            entry.setCaptivePortalAuthenticated(true);
        }

        return (local);
    }

    public boolean removeActiveNetUser(InetAddress netaddr)
    {
        CaptivePortalUserEntry user = null;
        String macaddr = null;

        HostTableEntry entry = UvmContextFactory.context().hostTable().getHostTableEntry(netaddr);
        if (entry != null) macaddr = entry.getMacAddress();

        if ((ownerApp.getSettings().getUseMacAddress()) && (macaddr != null)) {
            user = macAddrTable.get(macaddr);
            if (user != null) {
                logger.debug("REMOVE MAC TABLE: " + user.toString());
                macAddrTable.remove(user.getUserMacAddress());
            }
        } else {
            user = netAddrTable.get(netaddr);
            if (user != null) {
                logger.debug("REMOVE NET TABLE: " + user.toString());
                netAddrTable.remove(user.getUserNetAddress());
            }
        }

        if (user == null) return (false);

        // clear the capture username from the host table entry and turn
        // of the captive portal flag so it knows we are all done
        entry.setUsernameCapture(null);
        entry.setCaptivePortalAuthenticated(false);
        return (true);
    }

    public boolean removeActiveMacUser(String macaddr)
    {

        CaptivePortalUserEntry user = null;

        if ((ownerApp.getSettings().getUseMacAddress()) && (macaddr != null)) {
            user = macAddrTable.get(macaddr);
            if (user != null) {
                logger.debug("REMOVE MAC TABLE: " + user.toString());
                macAddrTable.remove(user.getUserMacAddress());
            }
        }

        if (user == null) return (false);

        // clear the capture username from the host table entry and turn
        // of the captive portal flag so it knows we are all done
        HostTableEntry entry = UvmContextFactory.context().hostTable().findHostTableEntry(macaddr);
        if (entry != null) {
            entry.setUsernameCapture(null);
            entry.setCaptivePortalAuthenticated(false);
        }

        return (true);
    }

    public CaptivePortalUserEntry searchByNetAddress(InetAddress netaddr)
    {
        CaptivePortalUserEntry user = null;
        String macaddr = null;

        HostTableEntry entry = UvmContextFactory.context().hostTable().getHostTableEntry(netaddr);
        if (entry != null) macaddr = entry.getMacAddress();

        if ((ownerApp.getSettings().getUseMacAddress()) && (macaddr != null)) {
            user = macAddrTable.get(entry.getMacAddress());
        } else {
            user = netAddrTable.get(netaddr);
        }

        return (user);
    }

    public CaptivePortalUserEntry searchByMacAddress(String macaddr)
    {
        CaptivePortalUserEntry user = null;

        if (ownerApp.getSettings().getUseMacAddress()) {
            user = macAddrTable.get(macaddr);
        }

        return (user);
    }

    public CaptivePortalUserEntry searchByUsername(String username, boolean ignoreCase)
    {
        Enumeration<CaptivePortalUserEntry> ee;

        // start with mac addr table if enabled in settings
        if (ownerApp.getSettings().getUseMacAddress()) {
            ee = macAddrTable.elements();

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
        }

        // not found in mac addr table so check net addr table
        ee = netAddrTable.elements();

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
        Enumeration<CaptivePortalUserEntry> ee;
        long currentTime, idleTrigger, idleTrigger2, userTrigger;
        int wipecount = 0;
        StaleUser stale;

        currentTime = (System.currentTimeMillis() / 1000);

        // start with mac addr table if enabled in settings
        if (ownerApp.getSettings().getUseMacAddress()) {

            ee = macAddrTable.elements();

            while (ee.hasMoreElements()) {
                CaptivePortalUserEntry item = ee.nextElement();
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
        }

        // now check all the entries in the net addr table
        ee = netAddrTable.elements();

        while (ee.hasMoreElements()) {
            CaptivePortalUserEntry item = ee.nextElement();
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
        if (ownerApp.getSettings().getUseMacAddress()) {
            macAddrTable.clear();
        }
        netAddrTable.clear();
    }
}
