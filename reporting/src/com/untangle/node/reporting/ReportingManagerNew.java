/**
 * $Id: ReportingManagerNew.java,v 1.00 2015/03/04 13:45:51 dmorris Exp $
 */
package com.untangle.node.reporting;

import java.util.ArrayList;
import java.util.Date;

import org.json.JSONObject;

/**
 * The API for interacting/viewing/editing reports
 */
public interface ReportingManagerNew
{
    ArrayList<ReportEntry> getReportEntries();

    ArrayList<ReportEntry> getReportEntries( String category );
    
    void setCustomReportEntries( ArrayList<ReportEntry> newEntries );

    ArrayList<JSONObject> getDataForReportEntry( ReportEntry entry, final Date startDate, final Date endDate, final int limit );
}
