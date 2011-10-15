/*
 * $Id$
 */
package com.untangle.node.reporting;

import java.io.IOException;

import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.Validator;

public interface ReportingNode extends Node
{
    void setReportingSettings(ReportingSettings settings);

    ReportingSettings getReportingSettings();

    void runDailyReport() throws Exception;

    Validator getValidator();

    /**
     * Force flush all the pending databdase events to the database and run
     * an incremental reports process
     *
     * force controls whether or not a minimum time is required to wait between calls
     * if true, it will flush events no matter what
     * if false, it will only flush events every set amount of time
     */
    void flushEvents(boolean force);

    /**
     * same as flushEvents(false)
     */
    void flushEvents();
    
    /**
     * Returns the current status string of a given reports run or incremental reports run
     * This can be used to display progress in the UI
     */
    String getCurrentStatus();
}
