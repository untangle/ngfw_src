/*
 * $HeadURL$
 */
package com.untangle.node.phish;

import com.untangle.uvm.logging.EventManager;
import com.untangle.node.spam.SpamNode;

public interface Phish extends SpamNode
{
    EventManager<PhishHttpEvent> getPhishHttpEventManager();

    void setPhishBaseSettings(PhishBaseSettings phishBaseSettings);
    PhishBaseSettings getPhishBaseSettings(boolean updateInfo);

    PhishBlockDetails getBlockDetails(String nonce);
    boolean unblockSite(String nonce, boolean global);
    String getUnblockMode();
}
