/**
 * $Id$
 */
package com.untangle.node.protofilter;

import com.untangle.uvm.node.EventLogQuery;
import com.untangle.uvm.node.Node;
import java.util.LinkedList;
import java.util.List;

public interface ProtoFilter extends Node
{
    ProtoFilterSettings getSettings();
    void setSettings(ProtoFilterSettings settings);

    int getPatternsTotal();
    int getPatternsLogged();
    int getPatternsBlocked();

    /**
     * Reconfigure node. This method should be called after some settings are updated
     * in order to reconfigure the node accordingly.
     *
     * @throws Exception if an exception occurs when reconfiguring.
     */
    void reconfigure() throws Exception;

    EventLogQuery[] getEventQueries();
}
