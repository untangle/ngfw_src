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

    void flushEvents();
    
    Validator getValidator();    
}
