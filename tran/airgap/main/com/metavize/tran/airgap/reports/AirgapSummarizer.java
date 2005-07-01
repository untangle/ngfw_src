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

package com.metavize.tran.airgap.reports;

import java.sql.*;

import com.metavize.mvvm.reporting.BaseSummarizer;
import com.metavize.mvvm.reporting.Util;

import org.apache.log4j.Logger;

public class AirgapSummarizer extends BaseSummarizer {

    private static final Logger logger = Logger.getLogger(AirgapSummarizer.class);

    public AirgapSummarizer() { }

    public String getSummaryHtml(Connection conn, Timestamp startDate, Timestamp endDate)
    {
	int sessionsRequested = 0;
	int sessionsAccepted = 0;
	int sessionsLimited = 0;
	int sessionsRejected = 0;
	int sessionsDropped = 0;
	
	float loadNormal = 0f;
	float loadMedium = 0f;
	float loadHigh = 0f;
	float loadOver = 0f;
	/*
        try {
	    String sql = "select count(*) from tr_protofilter_evt where time_stamp >= ? and time_stamp < ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            ResultSet rs = ps.executeQuery();
            rs.first();
            logCount = rs.getInt(1);
            rs.close();
            ps.close();

            sql = "select count(*) from tr_protofilter_evt where time_stamp >= ? and time_stamp < ? and blocked = 't'";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            blockCount = rs.getInt(1);
            rs.close();
            ps.close();
        } catch (SQLException exn) {
            logger.warn("could not summarize", exn);
        }
*/

        addEntry("Total sessions requested", Util.trimNumber("XXXX",sessionsRequested));
        addEntry("&nbsp;&nbsp;&nbsp;Accepted", Util.trimNumber("XXXX",sessionsAccepted));
        addEntry("&nbsp;&nbsp;&nbsp;Limited", Util.trimNumber("XXXX",sessionsLimited));
        addEntry("&nbsp;&nbsp;&nbsp;Rejected", Util.trimNumber("XXXX",sessionsRejected));
        addEntry("&nbsp;&nbsp;&nbsp;Dropped", Util.trimNumber("XXXX",sessionsDropped));
        addEntry("Load level indicators", "");
        addEntry("&nbsp;&nbsp;&nbsp;Normal operation", loadNormal + "% XXXX");
        addEntry("&nbsp;&nbsp;&nbsp;Medium load", loadMedium + "% XXXX");
        addEntry("&nbsp;&nbsp;&nbsp;High load", loadHigh + "% XXXX");
        addEntry("&nbsp;&nbsp;&nbsp;Overload", loadOver + "% XXXX");


        // XXXX
        String tranName = "Packet Attack Shield";

        return summarizeEntries(tranName);
    }
}

