/*
 * $HeadURL: svn://chef/work/src/uvm-lib/localapi/com/untangle/uvm/LocalUvmContext.java $
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

public abstract class AbstractReport
{
    protected static final String REPORT_DATE = "report-date";
    protected static final String ONE_DAY_BEFORE = "one-day-before";
    protected static final String ONE_WEEK_BEFORE = "one-week-before";

    public abstract List<Section> getSections();

    protected ResultSet doDateQuery(Connection c, String q, Timestamp startDate,
                                    Timestamp endDate)
        throws SQLException
    {
        PreparedStatement ps = c
            .prepareStatement("SELECT max(hits) AS max_hits, "
                              + "avg(hits) AS avg_hits "
                              + "FROM reports.n_http_totals "
                              + "WHERE trunc_time >= ? AND trunc_time < ?");
        ps.setTimestamp(1, startDate);
        ps.setTimestamp(2, endDate);
        return ps.executeQuery();
    }
}