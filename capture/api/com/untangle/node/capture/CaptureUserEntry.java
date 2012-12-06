/**
 * $Id: CaptureUserEntry.java,v 1.00 2011/12/14 01:02:03 mahotz Exp $
 */

package com.untangle.node.capture;

import java.net.InetAddress;

public class CaptureUserEntry
{
    private InetAddress userAddress;
    private String userName;
    private long sessionCreation;
    private long sessionActivity;
    private long sessionCounter;

    public CaptureUserEntry(InetAddress userAddress,String userName)
    {
        this.userAddress = userAddress;
        this.userName = userName;
        sessionCreation = System.currentTimeMillis();
        sessionActivity = sessionCreation;
    }

    public String getUserName()
    {
        return(userName);
    }

    public InetAddress getUserAddress()
    {
        return(userAddress);
    }

    public long getSessionCreation()
    {
        return(sessionCreation);
    }

    public long getSessionActivity()
    {
        return(sessionActivity);
    }

    public long getSessionCounter()
    {
        return(sessionCounter);
    }

    public void updateActivityTimer()
    {
        sessionActivity = System.currentTimeMillis();
        sessionCounter++;
    }
}
