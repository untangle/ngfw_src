/*
 * $HeadURL: svn://chef/work/src/webfilter-base/impl/com/untangle/node/webfilter/Blacklist.java $
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.webfilter;


import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.untangle.uvm.reports.AbstractReport;
import com.untangle.uvm.reports.KeyStatistic;
import com.untangle.uvm.reports.Section;
import com.untangle.uvm.util.I18nUtil;
import org.apache.log4j.Logger;

public class WebFilterReport extends AbstractReport
{
    private final Logger logger = Logger.getLogger(getClass());

    public List<Section> getSections()
    {
        List<Section> l = new ArrayList<Section>();

        return l;
    }

    private void hourlyWebUsageKey(Connection c, Map<String, Object> parameters)
        throws SQLException
    {
        Timestamp reportDate = (Timestamp)parameters.get(REPORT_DATE);
        Timestamp oneDay = (Timestamp)parameters.get(ONE_DAY_BEFORE);
        Timestamp oneWeek = (Timestamp)parameters.get(ONE_WEEK_BEFORE);

        String hitsQuery = "SELECT max(hits) AS max_hits, "
            + "avg(hits) AS avg_hits "
            + "FROM reports.n_http_totals "
            + "WHERE trunc_time >= ? AND trunc_time < ?";

        // Max hits/minute (1-day):
        // Avg hits/minute (1-day):
        ResultSet rs = doDateQuery(c, hitsQuery, oneDay, reportDate);

        int maxHitsOneDay = 0;
        double avgHitsOneDay = 0;

        if (rs.next()) {
            maxHitsOneDay = rs.getInt(1);
            avgHitsOneDay = rs.getDouble(2);
        } else {
            logger.warn("no result for daily hits");
        }


        // Max hits/minute (1-week):
        // Avg hits/minute (1-week):
        rs = doDateQuery(c, hitsQuery, oneWeek, reportDate);

        int maxHitsOneWeek = 0;
        double avgHitsOneWeek = 0;

        if (rs.next()) {
            maxHitsOneWeek = rs.getInt(1);
            avgHitsOneWeek = rs.getDouble(2);
        } else {
            logger.warn("no result for max_hits, avg_hits");
        }

        String violationsQuery = "SELECT avg(blocks) "
            + "FROM (SELECT date_trunc('hour', trunc_time) AS hour, "
            + "sum(blocks) AS blocks "
            + "FROM reports.n_http_totals "
            + "WHERE trunc_time >= ? "
            + "GROUP BY hour) AS foo";

        // Avg violations/hour (1-day):
        rs = doDateQuery(c, violationsQuery, oneDay, reportDate);

        double avgViolationsOneDay = 0;

        if (rs.next()) {
            avgViolationsOneDay = rs.getDouble(1);
        } else {
            logger.warn("no result for daily hits");
        }


        // Avg violations/hour (7-day):
        rs = doDateQuery(c, violationsQuery, oneWeek, reportDate);

        double avgViolationsOneWeek = 0;

        if (rs.next()) {
            avgViolationsOneWeek = rs.getDouble(1);
        } else {
            logger.warn("no result for weekly violations");
        }

        List<KeyStatistic> lks = new ArrayList<KeyStatistic>();
        KeyStatistic ks = new KeyStatistic(I18nUtil.marktr("Max hits 1-day"),
                                           maxHitsOneDay,
                                           I18nUtil.marktr("hits/minute"));
        lks.add(ks);
        ks = new KeyStatistic(I18nUtil.marktr("Avg hits 1-day"),
                              avgHitsOneDay,
                              I18nUtil.marktr("hits/minute"));
        lks.add(ks);
        ks = new KeyStatistic(I18nUtil.marktr("Max hits 7-day"),
                              maxHitsOneWeek,
                              I18nUtil.marktr("hits/minute"));
        lks.add(ks);
        ks = new KeyStatistic(I18nUtil.marktr("Avg hits 7-day"),
                              avgHitsOneWeek,
                              I18nUtil.marktr("hits/minute"));
        lks.add(ks);
        ks = new KeyStatistic(I18nUtil.marktr("Avg violations 1-day"),
                              avgViolationsOneDay,
                              I18nUtil.marktr("hits/hour"));
        lks.add(ks);
        ks = new KeyStatistic(I18nUtil.marktr("Avg violations 7-day"),
                              avgViolationsOneWeek,
                              I18nUtil.marktr("hits/hour"));
        lks.add(ks);
    }
}