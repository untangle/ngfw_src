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
        long currentTime = System.currentTimeMillis();
        int wipecount;
        int flag;

        Enumeration ee = userTable.elements();
        wipecount = 0;

        while (ee.hasMoreElements())
        {
            CaptureUserEntry item = (CaptureUserEntry)ee.nextElement();
            flag = 0;

                // look for users with no traffic within the configured idle timeout 
                if ( (idleTimeout > 0) && (currentTime > (item.grabActivityTime() + (idleTimeout * 1000))) )
                {
                    logger.debug("Idle timeout removing user " + item.getUserAddress() + " " + item.getUserName());
                    wipeList.add(item.getUserAddress());
                    wipecount++;
                    flag = 1;
                }

                // look for users who have exceeded the configured maximum session time
                if ( (userTimeout > 0) && (currentTime > (item.grabCreationTime() + (userTimeout * 1000))) )
                {
                    logger.debug("Session timeout removing user " + item.getUserAddress() + " " + item.getUserName());
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

    public CaptureUserEntry searchForUser(String search)
    {
        return(userTable.get(search));
    }
}
