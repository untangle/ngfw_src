/**
 * $Id$
 */
package com.untangle.uvm.app;

import java.util.List;
import java.sql.Connection;

import com.untangle.uvm.logging.LogEvent;

public interface Reporting
{
    void logEvent( LogEvent evt );

    void forceFlush();

    boolean partitionTableExists(String wantTableName);

    double getAvgWriteTimePerEvent();

    long getWriteDelaySec();

    boolean getDiskFullFlag();

    List<String> getAlertEmailAddresses();

    Connection getDbConnection();

    int getEventQueueSize();

}
