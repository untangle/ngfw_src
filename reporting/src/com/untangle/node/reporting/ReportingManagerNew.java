/**
 * $Id: ReportingManagerNew.java,v 1.00 2015/03/04 13:45:51 dmorris Exp $
 */
package com.untangle.node.reporting;

import java.util.List;
import java.util.Date;

import org.json.JSONObject;

import com.untangle.uvm.node.SqlCondition;

/**
 * The API for interacting/viewing/editing reports
 */
public interface ReportingManagerNew
{
    List<ReportEntry> getReportEntries();

    List<ReportEntry> getReportEntries( String category );
    
    void setReportEntries( List<ReportEntry> newEntries );

    List<JSONObject> getDataForReportEntry( ReportEntry entry, final Date startDate, final Date endDate, final int limit );

    List<JSONObject> getDataForReportEntry( ReportEntry entry, final Date startDate, final Date endDate, SqlCondition[] extraConditions, final int limit );
}
