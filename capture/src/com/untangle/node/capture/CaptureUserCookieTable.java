/**
 * $Id: CaptureUserCookieTable.java 39560 2015-01-22 20:46:37Z dmorris $
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
import com.untangle.uvm.HostTableEntry;

/*
 * When a user is forced to log out, their cookie must be expired.  
 * There's no way to push this, so the list of logged out users is 
 * maintained and the next time the user hits the handler script, 
 * the script checks and issues the "expire cookie".
 *
 * The table is not saved.
 */
public class CaptureUserCookieTable
{
    private final Logger logger = Logger.getLogger(getClass());
    private Hashtable<InetAddress, CaptureUserEntry> userTable;

    public CaptureUserCookieTable()
    {
        userTable = new Hashtable<InetAddress, CaptureUserEntry>();
    }

    public CaptureUserEntry insertInactiveUser( CaptureUserEntry local )
    {
        userTable.put(local.getUserAddress(), local);

        return (local);
    }

    public boolean removeActiveUser(InetAddress address)
    {
        CaptureUserEntry user = userTable.get(address);
        if (user == null) return (false);
        userTable.remove(address);

        return (true);
    }

    public CaptureUserEntry searchByAddressUsername(InetAddress address, String username)
    {
        CaptureUserEntry user = userTable.get(address);
        if( user != null && user.getUserName().equals( username ) ){
            return user;
        }
        return null;
    }

    public void purgeAllUsers()
    {
        userTable.clear();
    }
}
