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

package com.metavize.tran.reporting.reports;

import java.sql.*;

import com.metavize.mvvm.reporting.*;
import org.apache.log4j.Logger;

public class ReportingSummarizer extends BaseSummarizer {

    private static final Logger logger = Logger.getLogger(GeneralSummarizer.class);

    public ReportingSummarizer() { }

    public String getSummaryHtml(Connection conn, Timestamp startDate, Timestamp endDate)
    {

        int succeededCount = 0;
        int failedCount = 0;

        long c2pOut = 0;
        long p2sOut = 0;
        long s2pOut = 0;
        long p2cOut = 0;
	int numOut = 0;

        long c2pIn = 0;
        long p2sIn = 0;
        long s2pIn = 0;
        long p2cIn = 0;
	int numIn = 0;

        try {
            String sql = "SELECT sum(c2p_bytes), sum(p2s_bytes), sum(s2p_bytes), sum(p2c_bytes), count(*) FROM pl_endp JOIN pl_stats USING (session_id) WHERE client_intf = 1 AND raze_date >= ? AND create_date < ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            ResultSet rs = ps.executeQuery();
            rs.first();
            c2pOut = rs.getLong(1);
            p2sOut = rs.getLong(2);
            s2pOut = rs.getLong(3);
            p2cOut = rs.getLong(4);
        numOut = rs.getInt(5);
            rs.close();
            ps.close();

            sql = "SELECT sum(c2p_bytes), sum(p2s_bytes), sum(s2p_bytes), sum(p2c_bytes), count(*) FROM pl_endp JOIN pl_stats USING (session_id) WHERE client_intf = 0 AND raze_date >= ? AND create_date < ?";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            c2pIn = rs.getLong(1);
            p2sIn = rs.getLong(2);
            s2pIn = rs.getLong(3);
            p2cIn = rs.getLong(4);
        numIn = rs.getInt(5);
            rs.close();
            ps.close();

            sql = "select count(*) from mvvm_login_evt where time_stamp >= ? and time_stamp < ? and not local and succeeded";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            succeededCount = rs.getInt(1);
            rs.close();
            ps.close();

            sql = "select count(*) from mvvm_login_evt where time_stamp >= ? and time_stamp < ? and not local and not succeeded";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            failedCount = rs.getInt(1);
            rs.close();
            ps.close();

        } catch (SQLException exn) {
            logger.warn("could not summarize", exn);
        }

        long totalBytesOutgoing = p2sOut + p2cIn;
        long totalBytesIncoming = s2pOut + c2pIn;
        long totalTotal = totalBytesOutgoing + totalBytesIncoming;

        long totalOutboundBytes = p2sOut + p2cOut;
        long totalInboundBytes = c2pIn + s2pIn;

        long numSecs = (endDate.getTime() - startDate.getTime()) / 1000;
        double numDays = ((double)numSecs) / (60d * 60d * 24d);

        addEntry("Total data transferred", Util.trimNumber("Bytes",totalTotal));
        addEntry("&nbsp;&nbsp;&nbsp;Sent", Util.trimNumber("Bytes",totalBytesOutgoing) + " (" + Util.percentNumber(totalBytesOutgoing,totalTotal) + ")");
        addEntry("&nbsp;&nbsp;&nbsp;Received", Util.trimNumber("Bytes",totalBytesIncoming) + " (" + Util.percentNumber(totalBytesIncoming,totalTotal) + ")");

        addEntry("&nbsp;", "&nbsp;");

        addEntry("Total sessions created", Util.trimNumber("",numOut + numIn));
        addEntry("&nbsp;&nbsp;&nbsp;Outbound", Util.trimNumber("",numOut) + " (" + Util.percentNumber(numOut,numIn+numOut) + ")");
        addEntry("&nbsp;&nbsp;&nbsp;Inbound", Util.trimNumber("",numIn) + " (" + Util.percentNumber(numIn,numIn+numOut) + ")");

        addEntry("&nbsp;", "&nbsp;");

        addEntry("Data sent during outbound sessions", Util.trimNumber("Bytes",totalOutboundBytes));
        addEntry("Data sent during inbound sessions", Util.trimNumber("Bytes",totalInboundBytes));
        addEntry("Average data transfer rate", Util.trimNumber("Bytes/sec",(long) (((float)totalTotal) / numSecs)));
        addEntry("Daily data transfer rate", Util.trimNumber( "Bytes/day",(long) (((double)totalTotal) / numDays)));

        addEntry("&nbsp;", "&nbsp;");

        addEntry("Successful administrative logins", Util.trimNumber("",(long)succeededCount));
        addEntry("Failed administrative logins", Util.trimNumber("",(long)failedCount) );


        return summarizeEntries("Traffic Flow Rates");
    }

}


