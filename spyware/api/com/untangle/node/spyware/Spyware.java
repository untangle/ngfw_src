/*
 * $Id$
 */
package com.untangle.node.spyware;

import java.util.List;

import com.untangle.uvm.logging.EventManager;
import com.untangle.uvm.node.IPMaskedAddressRule;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.StringRule;
import com.untangle.uvm.node.Validator;

public interface Spyware extends Node
{
    SpywareSettings getSettings();

    void setSettings(SpywareSettings baseSettings);

    SpywareBlockDetails getBlockDetails(String nonce);

    boolean unblockSite(String nonce, boolean global);

    String getUnblockMode();

    EventManager<SpywareEvent> getEventManager();
}
