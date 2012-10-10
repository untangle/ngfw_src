/**
 * $Id: CaptureUserEntry.java,v 1.00 2011/12/14 01:02:03 mahotz Exp $
 */

package com.untangle.node.capture; // IMPL

public class CaptureUserEntry
{
    private String userAddress;
    private String userName;
    private String userPassword;
    private long sessionCreation;
    private long sessionActivity;
    
    public CaptureUserEntry(String userAddress,String userName,String userPassword)
    {
        this.userAddress = userAddress;
        this.userName = userName;
        this.userPassword = userPassword;
        sessionCreation = sessionActivity = System.currentTimeMillis();
    }
    
    public void updateActivityTimer()
    {
        sessionActivity = System.currentTimeMillis();
    }
    
    public long grabCreationTime()
    {
        return(sessionCreation);
    }
    
    public long grabActivityTime()
    {
        return(sessionActivity);
    }
    
    public String getUserAddress()
    {
        return(userAddress);
    }
    
    public String getUserName()
    {
        return(userName);
    }
}
