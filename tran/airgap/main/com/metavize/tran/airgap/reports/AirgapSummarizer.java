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
	long sessionsAccepted = 0;
	long sessionsLimited = 0;
	long sessionsRejected = 0;
	long sessionsDropped = 0;
	
	long loadRelaxed = 0l;
	long loadLax = 0l;
	long loadTight = 0l;
	long loadClosed = 0l;
	
        try {
	    String sql;
	    PreparedStatement ps;
	    ResultSet rs;

	    sql = "select sum(accepted), sum(limited), sum(rejected), sum(dropped), sum(relaxed), sum(lax), sum(tight), sum(closed) from shield_statistic_evt where time_stamp >= ? and time_stamp < ?";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            sessionsAccepted = rs.getLong(1);
	    sessionsLimited = rs.getLong(2);
	    sessionsRejected = rs.getLong(3);
	    sessionsDropped = rs.getLong(4);
	    loadRelaxed = rs.getLong(5);
	    loadLax = rs.getLong(6);
	    loadTight = rs.getLong(7);
	    loadClosed = rs.getLong(8);
            rs.close();
            ps.close();


        } catch (SQLException exn) {
            logger.warn("could not summarize", exn);
        }

	long sessionsRequested = sessionsAccepted + sessionsLimited + sessionsRejected + sessionsDropped;
        addEntry("Resource requests", Util.trimNumber("",sessionsRequested));
        addEntry("&nbsp;&nbsp;&nbsp;Accepted", Util.trimNumber("",sessionsAccepted), Util.percentNumber(sessionsAccepted,sessionsRequested));
        addEntry("&nbsp;&nbsp;&nbsp;Limited", Util.trimNumber("",sessionsLimited), Util.percentNumber(sessionsLimited,sessionsRequested));
        addEntry("&nbsp;&nbsp;&nbsp;Dropped", Util.trimNumber("",sessionsDropped), Util.percentNumber(sessionsDropped,sessionsRequested));
        addEntry("&nbsp;&nbsp;&nbsp;Rejected", Util.trimNumber("",sessionsRejected), Util.percentNumber(sessionsRejected,sessionsRequested));

        addEntry("&nbsp;", "&nbsp;");

	long loadTotal = loadRelaxed + loadLax + loadTight + loadClosed;
        addEntry("Resource allocation selectivity", "");
        addEntry("&nbsp;&nbsp;&nbsp;Normal", "", Util.percentNumber(loadRelaxed, loadTotal));
        addEntry("&nbsp;&nbsp;&nbsp;Increased", "", Util.percentNumber(loadLax, loadTotal));
        addEntry("&nbsp;&nbsp;&nbsp;High", "", Util.percentNumber(loadTight, loadTotal));
        addEntry("&nbsp;&nbsp;&nbsp;Defensive", "", Util.percentNumber(loadClosed, loadTotal));


        // XXXX
        String tranName = "Packet Attack Shield";

        return summarizeEntries(tranName);
    }
}

