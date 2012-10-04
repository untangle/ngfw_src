/**
 * $Id: CaptureNode.java,v 1.00 2011/12/27 09:42:36 mahotz Exp $
 */

package com.untangle.node.capture; // API

import java.util.LinkedList;
import java.util.List;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.EventLogQuery;

public interface CaptureNode extends Node
{
    CaptureSettings getSettings();
    void setSettings(CaptureSettings settings);

    EventLogQuery[] getEventQueries();
    EventLogQuery[] getRuleEventQueries();

    CaptureStatistics getStatistics();

    boolean userAuthenticate(String address, String username, String password);
    boolean userLogout(String address);
}
