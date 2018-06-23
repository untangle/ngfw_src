/**
 * $Id$
 */
package com.untangle.uvm;

import java.util.List;

import com.untangle.uvm.SessionMonitorEntry;

public interface SessionMonitor
{
    /**
     * Documented in SessionMonitorImpl
     */
    public List<SessionMonitorEntry> getMergedSessions();

    /**
     * Documented in SessionMonitorImpl
     */
    public List<SessionMonitorEntry> getMergedSessions(long appId);

    /**
     * Documented in SessionMonitorImpl
     */
    public org.json.JSONObject getSessionStats();

    /**
     * Documented in SessionMonitorImpl
     */
    public org.json.JSONObject getPoliciesSessionsStats();
}
