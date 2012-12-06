/**
 * $Id: CaptureUserTable.java,v 1.00 2011/12/14 01:02:03 mahotz Exp $
 */

package com.untangle.node.capture;

import java.net.InetAddress;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.TimerTask;
import org.apache.log4j.Logger;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.HostTable;

public class CaptureUserTable
{
    private final Logger logger = Logger.getLogger(getClass());
    private Hashtable<InetAddress,CaptureUserEntry> userTable;

    public class StaleUser
    {
        CaptureUserEvent.EventType reason;
        InetAddress address;

        StaleUser(InetAddress address,CaptureUserEvent.EventType reason)
        {
            this.address = address;
            this.reason = reason;
        }
    }

    public CaptureUserTable()
    {
        userTable = new Hashtable<InetAddress,CaptureUserEntry>();
    }

    public ArrayList<CaptureUserEntry> buildUserList()
    {
        ArrayList<CaptureUserEntry> userList = new ArrayList<CaptureUserEntry>(userTable.values());
        return(userList);
    }

    public CaptureUserEntry insertActiveUser(InetAddress address,String username)
    {
        CaptureUserEntry local = new CaptureUserEntry(address,username);
        userTable.put(address,local);

        UvmContextFactory.context().hostTable().getHostTableEntry( address, true).setUsernameCapture( username );
        return(local);
    }

    public boolean removeActiveUser(InetAddress address)
    {
        CaptureUserEntry user = userTable.get(address);
        if (user == null) return(false);
        userTable.remove(address);

        UvmContextFactory.context().hostTable().getHostTableEntry( address, true).setUsernameCapture( null );
        return(true);
    }

    public CaptureUserEntry searchByAddress(InetAddress address)
    {
        return(userTable.get(address));
    }

    public CaptureUserEntry searchByUsername(String username)
    {
        Enumeration ee = userTable.elements();

        while (ee.hasMoreElements())
        {
            CaptureUserEntry item = (CaptureUserEntry)ee.nextElement();
            if (username.equals(item.getUserName()) == true) return(item);
        }

        return(null);
    }

    public ArrayList<StaleUser> buildStaleList(long idleTimeout,long userTimeout)
    {
        ArrayList<StaleUser> wipelist = new ArrayList<StaleUser>();
        long currentTime,idleTrigger,userTrigger;
        StaleUser stale;
        int wipecount;

        currentTime = (System.currentTimeMillis() / 1000);
        Enumeration ee = userTable.elements();
        wipecount = 0;

            while (ee.hasMoreElements())
            {
            CaptureUserEntry item = (CaptureUserEntry)ee.nextElement();
            userTrigger = ((item.getSessionCreation() / 1000) + userTimeout);
            idleTrigger = ((item.getSessionActivity() / 1000) + idleTimeout);

                // look for users with no traffic within the configured non-zero idle timeout
                if ( (idleTimeout > 0) && (currentTime > idleTrigger) )
                {
                    logger.info("Idle timeout removing user " + item.getUserAddress() + " " + item.getUserName());
                    stale = new StaleUser(item.getUserAddress(),CaptureUserEvent.EventType.INACTIVE);
                    wipelist.add(stale);
                    wipecount++;
                }

                // look for users who have exceeded the configured maximum session time
                if (currentTime > userTrigger)
                {
                    logger.info("Session timeout removing user " + item.getUserAddress() + " " + item.getUserName());
                    stale = new StaleUser(item.getUserAddress(),CaptureUserEvent.EventType.TIMEOUT);
                    wipelist.add(stale);
                    wipecount++;
                }
            }

        return(wipelist);
    }
}
