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

    ArrayList<org.json.JSONObject> getEvents(final String query, final Long policyId, final SqlCondition[] extraConditions, final int limit);

    Object getEventsResultSet(final String query, final Long policyId, final SqlCondition[] extraConditions, final int limit, final Date startDate, final Date endDate);
    
    void createSchemas();

    double getAvgWriteTimePerEvent();

    long getWriteDelaySec();
}
