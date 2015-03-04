/**
 * $Id: ReportingManagerNew.java,v 1.00 2015/03/04 13:45:51 dmorris Exp $
 */
package com.untangle.node.reporting;

import java.util.ArrayList;

/**
 * The API for interacting/viewing/editing reports
 */
public interface ReportingManagerNew
{
    ArrayList<ReportEntry> getReportEntries();

    void setReportEntries( ArrayList<ReportEntry> newEntries );
}
