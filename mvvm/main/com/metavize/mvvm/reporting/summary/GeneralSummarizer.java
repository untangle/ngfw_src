/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: GeneralSummarizer.java,v 1.4 2005/02/12 00:54:02 jdi Exp $
 */

package com.metavize.mvvm.reporting.summary;

import com.metavize.mvvm.reporting.*;
import java.sql.*;
import java.util.Date;
import org.apache.log4j.Logger;

public class GeneralSummarizer implements ReportSummarizer {
    
    private static final Logger logger = Logger.getLogger(GeneralSummarizer.class);

    public GeneralSummarizer() { }

    public String getSummaryHtml(Connection conn, Timestamp startDate, Timestamp endDate)
    {
        StringBuilder result = new StringBuilder();
        emitHeader(result);

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

        result.append("<tr><td></td><td>Total bytes transferred</td><td>").append(totalTotal).append("</td> </tr>\n");
        result.append("<tr><td></td><td>Total bytes sent</td><td>").append(totalBytesOutgoing).append("</td> </tr>\n");
        result.append("<tr><td></td><td>Total bytes received</td><td>").append(totalBytesIncoming).append("</td> </tr>\n");
        result.append("<tr><td></td><td>Number of sessions</td><td>").append(numOut + numIn).append("</td> </tr>\n");
        result.append("<tr><td></td><td>Number of outbound sessions</td><td>").append(numOut).append("</td> </tr>\n");
        result.append("<tr><td></td><td>Total bytes for outbound sessions</td><td>").append(totalOutboundBytes).append("</td> </tr>\n");
        result.append("<tr><td></td><td>Number of inbound sessions</td><td>").append(numIn).append("</td> </tr>\n");
        result.append("<tr><td></td><td>Total bytes for inbound sessions</td><td>").append(totalInboundBytes).append("</td> </tr>\n");
        result.append("<tr><td></td><td>Average transfer rate (kB/sec)</td><td>").append(((float)totalTotal) / numSecs / 1024f).append("</td> </tr>\n");
        double bpd = ((double)totalTotal) / numDays / 1024d;
        result.append("<tr><td></td><td>Transferred per day (kB)</td><td>").append((int)bpd).append("</td> </tr>\n");
        return result.toString();
    }

    private void emitHeader(StringBuilder result)
    {
        result.append("<html>\n");
        result.append("<head><title>Metavize EdgeGuard Summary Report</title></head>\n");
        result.append("<body>\n");
        result.append("<table><tr>\n");
        result.append("<td><img src=\"LogoNoText64x64.gif\"/></td>\n");
        result.append("<td><b>Metavize EdgeGuard Summary Report</b></td>\n");
        result.append("</tr></table>\n");
        result.append(" <p/>\n");
        result.append("<table cellspacing=\"5\">\n");
        result.append("<tr><td colspan=\"2\"><b>General</b></td></tr>\n");
    }
}
