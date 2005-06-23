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

package com.metavize.mvvm.reporting.summary;

import com.metavize.mvvm.reporting.*;
import java.sql.*;
import java.util.Date;
import org.apache.log4j.Logger;

public class GeneralSummarizer extends BaseSummarizer {
    
    private static final Logger logger = Logger.getLogger(GeneralSummarizer.class);

    public GeneralSummarizer() { }

    public String getSummaryHtml(Connection conn, Timestamp startDate, Timestamp endDate)
    {
        StringBuilder result = new StringBuilder();


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
            String sql = "select sum(c2p_bytes), sum(p2s_bytes), sum(s2p_bytes), sum(p2c_bytes), count(*) from pipeline_info where client_intf = 1 and raze_date >= ? and create_date < ?";
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

            sql = "select sum(c2p_bytes), sum(p2s_bytes), sum(s2p_bytes), sum(p2c_bytes), count(*) from pipeline_info where client_intf = 0 and raze_date >= ? and create_date < ?";
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

        } catch (SQLException exn) {
            logger.warn("could not summarize", exn);
        }

        long totalBytesOutgoing = p2sOut + p2cIn;
        long totalBytesIncoming = s2pOut + c2pIn;
        long totalTotal = totalBytesOutgoing + totalBytesIncoming;

        long totalOutboundBytes = p2sOut + s2pOut;
        long totalInboundBytes = c2pIn + p2cIn;

        long numSecs = (endDate.getTime() - startDate.getTime()) / 1000;
        double numDays = ((double)numSecs) / (60d * 60d * 24d);

        addEntry("Total data transferred", Util.trimNumber("Bytes",totalTotal));
        addEntry("&nbsp;&nbsp;&nbsp;Data sent", Util.trimNumber("Bytes",totalBytesOutgoing));
        addEntry("&nbsp;&nbsp;&nbsp;Data received", Util.trimNumber("Bytes",totalBytesIncoming));
        addEntry("Total sessions created", Util.trimNumber("",numOut + numIn));
        addEntry("&nbsp;&nbsp;&nbsp;Outbound sessions created", Util.trimNumber("",numOut));
        addEntry("&nbsp;&nbsp;&nbsp;Inbound sessions created", Util.trimNumber("",numIn));
        addEntry("Data sent during outbound sessions", Util.trimNumber("Bytes",totalOutboundBytes));
        addEntry("Data sent during inbound sessions", Util.trimNumber("Bytes",totalInboundBytes));
        addEntry("Average data transfer rate", Util.trimNumber("Bytes/sec",(long) (((float)totalTotal) / numSecs)));
        addEntry("Daily data transfer rate", Util.trimNumber( "Bytes/day",(long) (((double)totalTotal) / numDays)));

        return summarizeEntries("Traffic Flow Rates");
    }

}
