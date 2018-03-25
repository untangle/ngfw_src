/**
 * $Id$
 */
package com.untangle.uvm.app;

import java.util.List;

import com.untangle.uvm.logging.LogEvent;

public interface Reporting
{
    void logEvent( LogEvent evt );

    void forceFlush();

    double getAvgWriteTimePerEvent();

    long getWriteDelaySec();

    List<String> getAlertEmailAddresses();
}
