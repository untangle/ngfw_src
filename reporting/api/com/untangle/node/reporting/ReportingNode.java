/*
 * $Id$
 */
package com.untangle.node.reporting;

import java.io.IOException;
import java.net.InetAddress;

import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.HostnameLookup;

public interface ReportingNode extends Node, HostnameLookup
{
    void setReportingSettings(ReportingSettings settings);

    ReportingSettings getReportingSettings();

    void runDailyReport() throws Exception;

    /**
     * Force flush all the pending databdase events to the database and run
     * an incremental reports process
     */
    void flushEvents();
    
    String lookupHostname( InetAddress address );
}
