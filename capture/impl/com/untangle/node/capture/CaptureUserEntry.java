/**
 * $Id: CaptureUserEntry.java,v 1.00 2011/12/14 01:02:03 mahotz Exp $
 */

package com.untangle.node.capture; // IMPL

public class CaptureUserEntry
{
    private String userAddress;
    private long sessionCreation;
    private long sessionActivity;
    
    public CaptureUserEntry(String userAddress)
    {
        this.userAddress = userAddress;
        this.sessionCreation = this.sessionActivity = System.currentTimeMillis();
    }
}
