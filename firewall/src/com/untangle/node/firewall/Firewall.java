/*
 * $Id$
 */
package com.untangle.node.firewall;

import java.util.List;

import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.EventLogQuery;

public interface Firewall extends Node
{
    FirewallSettings getSettings();

    void setSettings(FirewallSettings settings);

    /**
     * Convenience method for getting just the rules from the settings
     */
    List<FirewallRule> getRules();

    /**
     * Convenience method for setting just the rules in the settings
     */
    void setRules( List<FirewallRule> rules );

    EventLogQuery[] getEventQueries();
}
