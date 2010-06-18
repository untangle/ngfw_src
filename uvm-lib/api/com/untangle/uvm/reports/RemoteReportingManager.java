/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
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
public interface RemoteReportingManager
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
