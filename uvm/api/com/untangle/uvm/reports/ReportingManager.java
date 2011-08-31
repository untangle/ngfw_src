/*
 * $Id$
 */
package com.untangle.uvm.reports;

import java.util.Date;
import java.util.List;

/**
 * Manages report generation.
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
public interface ReportingManager
{
    List<DateItem> getDates();

    List<Highlight> getHighlights(Date d, int numDays);

    Date getReportsCutoff();

    TableOfContents getTableOfContents(Date d, int numDays);
    
    TableOfContents getTableOfContentsForHost(Date d, int numDays, String hostname);
    
    TableOfContents getTableOfContentsForUser(Date d, int numDays, String username);
    
    TableOfContents getTableOfContentsForEmail(Date d, int numDays, String email);

    ApplicationData getApplicationData(Date d, int numDays, String appName, String type, String value);
    
    ApplicationData getApplicationData(Date d, int numDays, String appName);

    ApplicationData getApplicationDataForUser(Date d, int numDays, String appName, String username);

    ApplicationData getApplicationDataForEmail(Date d, int numDays, String appName, String emailAddr);

    ApplicationData getApplicationDataForHost(Date d, int numDays, String appName, String hostname);

    List<List<Object>> getDetailData(Date d, int numDays, String appName, String detailName, String type, String value);

    List<List<Object>> getAllDetailData(Date d, int numDays, String appName, String detailName, String type, String value);

    // old stuff ---------------------------------------------------------------

    /**
     * Tests if reporting is enabled, that is if reports will be
     * generated nightly.  Currently this is the same thing as "is the
     * reporting node installed and turned on."
     *
     * @return true if reporting is enabled, false otherwise.
     */
    boolean isReportingEnabled();

    /**
     * Tests if reporting is enabled and reports have been generated
     * and are ready to view.  Currently this is the same thing as
     * "does the current symlink exist and contain a valid
     * reporting-node/sum-daily.html file."
     *
     * @return true if reports are available
     */
    boolean isReportsAvailable();
}
