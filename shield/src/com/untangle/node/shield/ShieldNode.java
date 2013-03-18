/*
 * $Id$
 */
package com.untangle.node.shield;

import java.util.List;

import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.EventLogQuery;

public interface ShieldNode extends Node
{
    void setSettings(ShieldSettings baseSettings);

    ShieldSettings getSettings();

    EventLogQuery[] getEventQueries();
}
