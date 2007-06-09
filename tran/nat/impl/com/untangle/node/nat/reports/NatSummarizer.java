/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.node.nat.reports;

import java.sql.*;

import com.untangle.uvm.reporting.BaseSummarizer;
import com.untangle.uvm.reporting.Util;
import org.apache.log4j.Logger;

public class NatSummarizer extends BaseSummarizer {

    private final Logger logger = Logger.getLogger(getClass());

    public NatSummarizer() { }

    public String getSummaryHtml(Connection conn, Timestamp startDate, Timestamp endDate)
    {
        long totalOutboundSessionsCreated = 0l;
        long totalRedirections = 0l;
        long tcpRedirections = 0l;
        long tcpInboundRedirections = 0l;
        long tcpOutboundRedirections = 0l;
        long udpRedirections = 0l;
        long udpInboundRedirections = 0l;
        long udpOutboundRedirections = 0l;
        long pingRedirections = 0l;
        long pingInboundRedirections = 0l;
        long pingOutboundRedirections = 0l;
        long dmzRedirections = 0l;

        try {
            String sql;
            PreparedStatement ps;
            ResultSet rs;

            sql = " SELECT SUM(nat_sessions), SUM(dmz_sessions)," +
                " SUM(tcp_incoming), SUM(tcp_outgoing)," +
                " SUM(udp_incoming), SUM(udp_outgoing)," +
                " SUM(icmp_incoming), SUM(icmp_outgoing)" +
                " FROM tr_nat_statistic_evt WHERE time_stamp >= ? AND time_stamp < ?";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            totalOutboundSessionsCreated = rs.getLong(1);
            dmzRedirections = rs.getLong(2);
            tcpInboundRedirections = rs.getLong(3);
            tcpOutboundRedirections = rs.getLong(4);
            udpInboundRedirections = rs.getLong(5);
            udpOutboundRedirections = rs.getLong(6);
            pingInboundRedirections = rs.getLong(7);
            pingOutboundRedirections = rs.getLong(8);
            rs.close();
            ps.close();

        } catch (SQLException exn) {
            logger.warn("could not summarize", exn);
        }

        tcpRedirections = tcpInboundRedirections + tcpOutboundRedirections;
        udpRedirections = udpInboundRedirections + udpOutboundRedirections;
        pingRedirections = pingInboundRedirections + pingOutboundRedirections;
        totalRedirections = tcpRedirections + udpRedirections + pingRedirections;

        addEntry("Total redirections", Util.trimNumber("",totalRedirections) );

        addEntry("&nbsp;","&nbsp;");

        addEntry("TCP redirections", Util.trimNumber("",tcpRedirections));
        addEntry("&nbsp;&nbsp;&nbsp;Inbound", Util.trimNumber("",tcpInboundRedirections),
                 Util.percentNumber(tcpInboundRedirections, totalRedirections));
        addEntry("&nbsp;&nbsp;&nbsp;Outbound", Util.trimNumber("",tcpOutboundRedirections),
                 Util.percentNumber(tcpOutboundRedirections, totalRedirections));
        addEntry("UDP redirections", Util.trimNumber("",udpRedirections));
        addEntry("&nbsp;&nbsp;&nbsp;Inbound", Util.trimNumber("",udpInboundRedirections),
                 Util.percentNumber(udpInboundRedirections, totalRedirections));
        addEntry("&nbsp;&nbsp;&nbsp;Outbound", Util.trimNumber("",udpOutboundRedirections),
                 Util.percentNumber(udpOutboundRedirections, totalRedirections));
        addEntry("PING redirections", Util.trimNumber("",pingRedirections));
        addEntry("&nbsp;&nbsp;&nbsp;Inbound", Util.trimNumber("",pingInboundRedirections),
                 Util.percentNumber(pingInboundRedirections, totalRedirections));
        addEntry("&nbsp;&nbsp;&nbsp;Outbound", Util.trimNumber("",pingOutboundRedirections),
                 Util.percentNumber(pingOutboundRedirections, totalRedirections));

        addEntry("&nbsp;","&nbsp;");

        addEntry("NAT outbound sessions created", Util.trimNumber("",totalOutboundSessionsCreated));
        addEntry("DMZ Host inbound redirections", Util.trimNumber("",dmzRedirections) );

        // XXXX
        String tranName = "Router";

        return summarizeEntries(tranName);
    }
}
