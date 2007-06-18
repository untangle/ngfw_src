/*
 * $HeadURL$
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

package com.untangle.node.reporting.reports;

import java.sql.*;

import com.untangle.uvm.reporting.*;
import org.apache.log4j.Logger;

public class ReportingSummarizer extends BaseSummarizer {
    private final Logger logger = Logger.getLogger(getClass());

    public ReportingSummarizer() { }

    public String getSummaryHtml(Connection conn, Timestamp startDate, Timestamp endDate)
    {

        long succeededCount = 0;
        long failedCount = 0;

        long c2pOut = 0;
        long p2sOut = 0;
        long s2pOut = 0;
        long p2cOut = 0;
        long numOut = 0;

        long c2pIn = 0;
        long p2sIn = 0;
        long s2pIn = 0;
        long p2cIn = 0;
        long numIn = 0;

        try {
            String sql = "SELECT SUM(c2p_bytes), SUM(p2s_bytes), SUM(s2p_bytes), SUM(p2c_bytes), COUNT(*) FROM pl_endp endp JOIN pl_stats stats ON (endp.event_id = stats.pl_endp_id) WHERE client_intf = 1 AND stats.time_stamp >= ? AND endp.time_stamp < ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            ResultSet rs = ps.executeQuery();
            rs.first();
            c2pOut = rs.getLong(1);
            p2sOut = rs.getLong(2);
            s2pOut = rs.getLong(3);
            p2cOut = rs.getLong(4);
            numOut = rs.getLong(5);
            rs.close();
            ps.close();

            sql = "SELECT sum(c2p_bytes), sum(p2s_bytes), sum(s2p_bytes), sum(p2c_bytes), count(*) FROM pl_endp endp JOIN pl_stats stats ON endp.event_id = stats.pl_endp_id WHERE client_intf = 0 AND stats.time_stamp >= ? AND endp.time_stamp < ?";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            c2pIn = rs.getLong(1);
            p2sIn = rs.getLong(2);
            s2pIn = rs.getLong(3);
            p2cIn = rs.getLong(4);
            numIn = rs.getLong(5);
            rs.close();
            ps.close();

            sql = "select count(*) from u_login_evt where time_stamp >= ? and time_stamp < ? and not local and succeeded";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            succeededCount = rs.getLong(1);
            rs.close();
            ps.close();

            sql = "select count(*) from u_login_evt where time_stamp >= ? and time_stamp < ? and not local and not succeeded";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            failedCount = rs.getLong(1);
            rs.close();
            ps.close();

        } catch (SQLException exn) {
            logger.warn("could not summarize", exn);
        }

        long bytesReceivedFromOutside = s2pOut + c2pIn;
        long bytesSentToOutside = p2sOut + p2cIn;
        long bytesTotalByDirection = bytesReceivedFromOutside + bytesSentToOutside;

        long totalOutboundBytes = p2sOut + p2cOut;
        long totalInboundBytes = c2pIn + s2pIn;

        double numSecs = (endDate.getTime() - startDate.getTime()) / 1000d;
        double numDays = ((double)numSecs) / (60d * 60d * 24d);


        addEntry("Average data transfer rates", "&nbsp;");
        addEntry("&nbsp;&nbsp;&nbsp;Per second", Util.trimNumber("Bytes/sec",(long) (((double)bytesTotalByDirection) / numSecs)));
        addEntry("&nbsp;&nbsp;&nbsp;Per day", Util.trimNumber( "Bytes/day",(long) (((double)bytesTotalByDirection) / numDays)));

        addEntry("&nbsp;", "&nbsp;");

        addEntry("Data transferred", Util.trimNumber("Bytes",bytesTotalByDirection));
        addEntry("&nbsp;&nbsp;&nbsp;Outbound", Util.trimNumber("Bytes",bytesSentToOutside), Util.percentNumber(bytesSentToOutside,bytesTotalByDirection));
        addEntry("&nbsp;&nbsp;&nbsp;Inbound", Util.trimNumber("Bytes",bytesReceivedFromOutside), Util.percentNumber(bytesReceivedFromOutside,bytesTotalByDirection));

        addEntry("&nbsp;", "&nbsp;");

        addEntry("Sessions created", Util.trimNumber("",numOut + numIn));
        addEntry("&nbsp;&nbsp;&nbsp;Outbound", Util.trimNumber("",numOut), Util.percentNumber(numOut,numIn+numOut));
        addEntry("&nbsp;&nbsp;&nbsp;Inbound", Util.trimNumber("",numIn), Util.percentNumber(numIn,numIn+numOut));

        addEntry("&nbsp;", "&nbsp;");

        addEntry("Administrative logins", succeededCount+failedCount);
        addEntry("&nbsp;&nbsp;&nbsp;Successful", Util.trimNumber("",succeededCount), Util.percentNumber(succeededCount,succeededCount+failedCount));
        addEntry("&nbsp;&nbsp;&nbsp;Failed", Util.trimNumber("",failedCount), Util.percentNumber(failedCount,succeededCount+failedCount));


        return summarizeEntries("Traffic Flow Rates");
    }

}


