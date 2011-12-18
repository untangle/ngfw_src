/*
 * $HeadURL$
 */
package com.untangle.node.phish;

import com.untangle.uvm.node.EventLogQuery;
import com.untangle.node.spam.SpamNode;

public interface Phish extends SpamNode
{
    public EventLogQuery[] getHttpEventQueries();

    PhishSettings getSettings();
    void setSettings(PhishSettings phishSettings);
    
    PhishBlockDetails getBlockDetails(String nonce);
    boolean unblockSite(String nonce, boolean global);
    String getUnblockMode();
}
