/**
 * $Id$
 */
package com.untangle.node.shield;

import java.util.List;

import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.EventLogEntry;

public interface ShieldNode extends Node
{
    void setSettings( ShieldSettings settings );

    ShieldSettings getSettings();

    EventLogEntry[] getEventQueries();
}
