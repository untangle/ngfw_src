/**
 * $Id$
 */
package com.untangle.node.ips;

import java.util.List;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.EventLogQuery;

public interface IpsNode extends Node
{
    EventLogQuery[] getEventQueries();

    IpsStatistics getStatistics();

    IpsSettings getSettings();
    void setSettings(IpsSettings settings);
}
