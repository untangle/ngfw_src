/**
 * $Id$
 */
package com.untangle.uvm.node;

import java.util.ArrayList;
import java.util.Date;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.node.SqlCondition;

public interface Reporting
{
    void logEvent( LogEvent evt );

    void forceFlush();

    void createSchemas();

    double getAvgWriteTimePerEvent();

    long getWriteDelaySec();
}
