/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.spyware.reports;

import java.sql.*;

import com.metavize.mvvm.reporting.BaseSummarizer;
import com.metavize.mvvm.reporting.Util;
import org.apache.log4j.Logger;

public class SpywareSummarizer extends BaseSummarizer {

    private static final Logger logger = Logger.getLogger(SpywareSummarizer.class);

    public SpywareSummarizer() { }

    public String getSummaryHtml(Connection conn, Timestamp startDate, Timestamp endDate)
    {
        int accessCount = 0;
        int blockCount = 0;
        int cookieCount = 0;
        int axCount = 0;

        try {
	    String sql;
	    PreparedStatement ps;
	    ResultSet rs;

            sql = "select count(*) from tr_spyware_evt_access where time_stamp >= ? and time_stamp < ?";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            accessCount = rs.getInt(1);
            rs.close();
            ps.close();

            sql = "select count(*) from tr_spyware_evt_access where time_stamp >= ? and time_stamp < ? and blocked = 't'";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            blockCount = rs.getInt(1);
            rs.close();
            ps.close();

            sql = "select count(*) from tr_spyware_evt_cookie where time_stamp >= ? and time_stamp < ?";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            cookieCount = rs.getInt(1);
            rs.close();
            ps.close();

            sql = "select count(*) from tr_spyware_evt_activex where time_stamp >= ? and time_stamp < ?";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            axCount = rs.getInt(1);
            rs.close();
            ps.close();

        } catch (SQLException exn) {
            logger.warn("could not summarize", exn);
        }

        addEntry("Potential spyware communications detected", Util.trimNumber("",accessCount));
        addEntry("&nbsp;&nbsp;&nbsp;Blocked spyware", Util.trimNumber("",blockCount) + " (" + Util.percentNumber(blockCount,accessCount)  + ")");
        addEntry("&nbsp;&nbsp;&nbsp;Blocked cookies", Util.trimNumber("",cookieCount) + " (" + Util.percentNumber(cookieCount,accessCount)  + ")");
        addEntry("&nbsp;&nbsp;&nbsp;Blocked ActiveX", Util.trimNumber("",axCount) + " (" + Util.percentNumber(axCount,accessCount)  + ")");
        addEntry("&nbsp;&nbsp;&nbsp;Passed", Util.trimNumber("",accessCount-blockCount-cookieCount-axCount) + " (" + Util.percentNumber(accessCount-blockCount-cookieCount-axCount,accessCount)  + ")");

        // XXXX
        String tranName = "Spyware Blocker";

        return summarizeEntries(tranName);
    }
}

