/*
 * $Id$
 */
package com.untangle.node.firewall;

import java.util.List;

import com.untangle.uvm.logging.EventManager;
import com.untangle.uvm.node.Node;

public interface Firewall extends Node
{
    EventManager<FirewallEvent> getEventManager();

    FirewallSettings getSettings();

    void setSettings(FirewallSettings settings);
}
