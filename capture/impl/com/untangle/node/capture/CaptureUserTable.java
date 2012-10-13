/**
 * $Id: CaptureUserTable.java,v 1.00 2011/12/14 01:02:03 mahotz Exp $
 */

package com.untangle.node.capture; // IMPL

import java.awt.List;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.TimerTask;
import org.apache.log4j.Logger;

public class CaptureUserTable
{
    private final Logger logger = Logger.getLogger(getClass());
    private Hashtable<String,CaptureUserEntry> userTable;

    public CaptureUserTable()
    {
        userTable = new Hashtable<String,CaptureUserEntry>();
    }

    public synchronized int cleanupStaleUsers(long idleTimeout,long userTimeout)
    {
        ArrayList<String> wipeList = new ArrayList<String>();
        long currentTime,idleTrigger,userTrigger;
        int wipecount;
        int flag;

        currentTime = (System.currentTimeMillis() / 1000);
        Enumeration ee = userTable.elements();
        wipecount = 0;

        while (ee.hasMoreElements())
        {
            CaptureUserEntry item = (CaptureUserEntry)ee.nextElement();
            idleTrigger = ((item.grabActivityTime() / 1000) + idleTimeout);
            userTrigger = ((item.grabCreationTime() / 1000) + userTimeout);
            flag = 0;

            logger.debug("CURR:" + currentTime + " IDLE:" + idleTrigger + " USER:" + userTrigger);

                // look for users with no traffic within the configured non-zero idle timeout
                if ( (idleTimeout > 0) && (currentTime > idleTrigger) )
                {
                    logger.info("Idle timeout removing user " + item.getUserAddress() + " " + item.getUserName());
                    wipeList.add(item.getUserAddress());
                    wipecount++;
                    flag = 1;
                }

                // look for users who have exceeded the configured maximum session time
                if (currentTime > userTrigger)
                {
                    logger.info("Session timeout removing user " + item.getUserAddress() + " " + item.getUserName());
                    wipeList.add(item.getUserAddress());
                    wipecount++;
                    flag = 2;
                }

                if (flag == 0)
                {
                    logger.debug("Keeping active user " + item.getUserAddress() + " " + item.getUserName());
                }
        }

        for(String item: wipeList)
        {
            userTable.remove(item);
        }

        return(wipecount);
    }

    public CaptureUserEntry insertActiveUser(String address,String username,String password)
    {
        CaptureUserEntry local = new CaptureUserEntry(address,username,password);
        userTable.put(address,local);
        return(local);
    }

    public boolean removeActiveUser(String address)
    {
        CaptureUserEntry user = userTable.get(address);
        if (user == null) return(false);
        userTable.remove(address);
        return(true);
    }

    public CaptureUserEntry searchByAddress(String address)
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
}
