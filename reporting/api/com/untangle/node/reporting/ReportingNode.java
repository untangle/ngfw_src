/*
 * $Id$
 */
package com.untangle.node.reporting;

import java.io.IOException;
import java.net.InetAddress;

import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.HostnameLookup;
import com.untangle.uvm.node.Validator;

public interface ReportingNode extends Node, HostnameLookup
{
    void setReportingSettings(ReportingSettings settings);

    ReportingSettings getReportingSettings();

    void runDailyReport() throws Exception;

    Validator getValidator();

    /**
     * Force flush all the pending databdase events to the database and run
     * an incremental reports process
     */
    void flushEvents();
    
    /**
     * Returns the current status string of a given reports run or incremental reports run
     * This can be used to display progress in the UI
     */
    String getCurrentStatus();

    String lookupHostname( InetAddress address );
}
