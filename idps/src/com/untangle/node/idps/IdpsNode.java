/**
 * $Id: IdpsNode.java 31685 2012-04-15 15:50:30Z mahotz $
 */
package com.untangle.node.idps;

import java.util.List;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.EventLogQuery;

public interface IdpsNode extends Node
{
    EventLogQuery[] getEventQueries();

    public String getSettingsFileName();
    public void initializeSettings();
    public void saveSettings( String tempFileName );

}
