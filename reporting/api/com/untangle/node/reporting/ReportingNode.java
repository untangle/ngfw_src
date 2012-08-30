/*
 * $Id$
 */
package com.untangle.node.reporting;

import java.util.ArrayList;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.ResultSet;

import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.HostnameLookup;

public interface ReportingNode extends Node, HostnameLookup
{
    void setSettings(ReportingSettings settings);

    ReportingSettings getSettings();

    void runDailyReport() throws Exception;

    void flushEvents();
    
    String lookupHostname( InetAddress address );

    ReportingManager getReportingManager();

    Connection getDbConnection();

    ArrayList getEvents( final String query, final Long policyId, final int limit );

    ResultSet getEventsResultSet( final String query, final Long policyId, final int limit );
}
