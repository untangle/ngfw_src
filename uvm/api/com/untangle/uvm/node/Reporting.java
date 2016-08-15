/**
 * $Id$
 */
package com.untangle.uvm.node;

import java.util.ArrayList;
import java.util.Date;

import com.untangle.uvm.logging.LogEvent;

public interface Reporting
{
    void logEvent( LogEvent evt );

    void forceFlush();

    double getAvgWriteTimePerEvent();

    long getWriteDelaySec();
}
