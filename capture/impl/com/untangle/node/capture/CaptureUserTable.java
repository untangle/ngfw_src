/**
 * $Id: CaptureUserTable.java,v 1.00 2011/12/14 01:02:03 mahotz Exp $
 */

package com.untangle.node.capture; // IMPL

import java.util.TimerTask;
import java.util.Hashtable;
import org.apache.log4j.Logger;

public class CaptureUserTable
{
    private final Logger logger = Logger.getLogger(getClass());
    
    private Hashtable<String,CaptureUserEntry> userTable;

    public CaptureUserTable()
    {
        this.userTable = new Hashtable<String,CaptureUserEntry>();
    }
    
    public CaptureUserEntry searchTable(String search)
    {
        return(userTable.get(search));
    }
}
