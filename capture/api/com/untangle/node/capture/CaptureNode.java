/**
 * $Id: CaptureNode.java,v 1.00 2011/12/27 09:42:36 mahotz Exp $
 */

package com.untangle.node.capture; // API

import java.util.ArrayList;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.EventLogQuery;

public interface CaptureNode extends Node
{
    CaptureSettings getSettings();
    void setSettings(CaptureSettings settings);
    
    ArrayList<CaptureUserEntry> getActiveUsers();

    EventLogQuery[] getEventQueries();
    EventLogQuery[] getRuleEventQueries();

    int userAuthenticate(String address, String username, String password);
    int userActivate(String address, String agree);
    int userLogout(String address);
}
