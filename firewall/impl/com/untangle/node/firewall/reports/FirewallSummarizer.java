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

package com.untangle.node.firewall.reports;

import java.sql.*;

import com.untangle.uvm.reporting.BaseSummarizer;
import com.untangle.uvm.reporting.Util;
import org.apache.log4j.Logger;

public class FirewallSummarizer extends BaseSummarizer {

    private final Logger logger = Logger.getLogger(getClass());

    public FirewallSummarizer() { }

    public String getSummaryHtml(Connection conn, Timestamp startDate, Timestamp endDate)
    {
        long totalSessionsBlocked = 0l;
        long totalSessionsBlockedRule = 0l;
        long totalSessionsBlockedDefault = 0l;
        long totalSessionsPassed = 0l;
        long totalSessionsPassedRule = 0l;
        long totalSessionsPassedDefault = 0l;

        long totalSessionsExamined = 0l;
        long tcpSessionsBlockedDefault = 0l;
        long tcpSessionsBlockedRule = 0l;
        long tcpSessionsPassedDefault = 0l;
        long tcpSessionsPassedRule = 0l;
        long udpSessionsBlockedDefault = 0l;
        long udpSessionsBlockedRule = 0l;
        long udpSessionsPassedDefault = 0l;
        long udpSessionsPassedRule = 0l;
        long pingSessionsBlockedDefault = 0l;
        long pingSessionsBlockedRule = 0l;
        long pingSessionsPassedDefault = 0l;
        long pingSessionsPassedRule = 0l;

        try {
            String sql;
            PreparedStatement ps;
            ResultSet rs;

            sql = " SELECT SUM(TCP_BLOCK_DEFAULT),  SUM(TCP_BLOCK_RULE)," +
                " SUM(TCP_PASS_DEFAULT),   SUM(TCP_PASS_RULE)," +
                " SUM(UDP_BLOCK_DEFAULT),  SUM(UDP_BLOCK_RULE)," +
                " SUM(UDP_PASS_DEFAULT),   SUM(UDP_PASS_RULE)," +
                " SUM(ICMP_BLOCK_DEFAULT), SUM(ICMP_BLOCK_RULE)," +
                " SUM(ICMP_PASS_DEFAULT),  SUM(ICMP_PASS_RULE)" +
                " FROM n_firewall_statistic_evt WHERE time_stamp >= ? and time_stamp < ?";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            tcpSessionsBlockedDefault = rs.getLong(1);
            tcpSessionsBlockedRule = rs.getLong(2);
            tcpSessionsPassedDefault = rs.getLong(3);
            tcpSessionsPassedRule = rs.getLong(4);
            udpSessionsBlockedDefault = rs.getLong(5);
            udpSessionsBlockedRule = rs.getLong(6);
            udpSessionsPassedDefault = rs.getLong(7);
            udpSessionsPassedRule = rs.getLong(8);
            pingSessionsBlockedDefault = rs.getLong(9);
            pingSessionsBlockedRule = rs.getLong(10);
            pingSessionsPassedDefault = rs.getLong(11);
            pingSessionsPassedRule = rs.getLong(12);
            rs.close();
            ps.close();

        } catch (SQLException exn) {
            logger.warn("could not summarize", exn);
        }

        totalSessionsPassedDefault = tcpSessionsPassedDefault + udpSessionsPassedDefault + pingSessionsPassedDefault;
        totalSessionsPassedRule = tcpSessionsPassedRule + udpSessionsPassedRule + pingSessionsPassedRule;
        totalSessionsPassed = totalSessionsPassedDefault + totalSessionsPassedRule;
        totalSessionsBlockedDefault = tcpSessionsBlockedDefault + udpSessionsBlockedDefault + pingSessionsBlockedDefault;
        totalSessionsBlockedRule = tcpSessionsBlockedRule + udpSessionsBlockedRule + pingSessionsBlockedRule;
        totalSessionsBlocked = totalSessionsBlockedDefault + totalSessionsBlockedRule;
        totalSessionsExamined = totalSessionsBlocked + totalSessionsPassed;

        addEntry("Sessions examined", Util.trimNumber("",totalSessionsExamined));

        addEntry("&nbsp;","&nbsp;");

        addEntry("Sessions blocked", Util.trimNumber("",totalSessionsBlocked));
        addEntry("&nbsp;&nbsp;&nbsp;Blocked by rule", Util.trimNumber("",totalSessionsBlockedRule),
                 Util.percentNumber(totalSessionsBlockedRule, totalSessionsExamined));
        addEntry("&nbsp;&nbsp;&nbsp;Blocked by default", Util.trimNumber("",totalSessionsBlockedDefault),
                 Util.percentNumber(totalSessionsBlockedDefault, totalSessionsExamined));
        addEntry("Sessions passed", Util.trimNumber("",totalSessionsPassed));
        addEntry("&nbsp;&nbsp;&nbsp;Passed by rule", Util.trimNumber("",totalSessionsPassedRule),
                 Util.percentNumber(totalSessionsPassedRule, totalSessionsExamined));
        addEntry("&nbsp;&nbsp;&nbsp;Passed by default", Util.trimNumber("",totalSessionsPassedDefault),
                 Util.percentNumber(totalSessionsPassedDefault, totalSessionsExamined));

        addEntry("&nbsp;", "&nbsp;");

        addEntry("TCP sessions",  Util.trimNumber("",tcpSessionsBlockedRule+tcpSessionsBlockedDefault+tcpSessionsPassedRule+tcpSessionsPassedDefault));
        addEntry("&nbsp;&nbsp;&nbsp;Blocked", Util.trimNumber("",tcpSessionsBlockedRule+tcpSessionsBlockedDefault),
                 Util.percentNumber(tcpSessionsBlockedRule+tcpSessionsBlockedDefault, totalSessionsExamined));
        addEntry("&nbsp;&nbsp;&nbsp;Passed", Util.trimNumber("",tcpSessionsPassedRule+tcpSessionsPassedDefault),
                 Util.percentNumber(tcpSessionsPassedRule+tcpSessionsPassedDefault, totalSessionsExamined));
        addEntry("UDP sessions",  Util.trimNumber("",udpSessionsBlockedRule+udpSessionsBlockedDefault+udpSessionsPassedRule+udpSessionsPassedDefault));
        addEntry("&nbsp;&nbsp;&nbsp;Blocked", Util.trimNumber("",udpSessionsBlockedRule+udpSessionsBlockedDefault),
                 Util.percentNumber(udpSessionsBlockedRule+udpSessionsBlockedDefault, totalSessionsExamined));
        addEntry("&nbsp;&nbsp;&nbsp;Passed", Util.trimNumber("",udpSessionsPassedRule+udpSessionsPassedDefault),
                 Util.percentNumber(udpSessionsPassedRule+udpSessionsPassedDefault, totalSessionsExamined));
        addEntry("PING sessions",  Util.trimNumber("",pingSessionsBlockedRule+pingSessionsBlockedDefault+pingSessionsPassedRule+pingSessionsPassedDefault));
        addEntry("&nbsp;&nbsp;&nbsp;Blocked", Util.trimNumber("",pingSessionsBlockedRule+pingSessionsBlockedDefault),
                 Util.percentNumber(pingSessionsBlockedRule+pingSessionsBlockedDefault, totalSessionsExamined));
        addEntry("&nbsp;&nbsp;&nbsp;Passed", Util.trimNumber("",pingSessionsPassedRule+pingSessionsPassedDefault),
                 Util.percentNumber(pingSessionsPassedRule+pingSessionsPassedDefault, totalSessionsExamined));

        // XXXX
        String tranName = "Firewall";

        return summarizeEntries(tranName);
    }
}
