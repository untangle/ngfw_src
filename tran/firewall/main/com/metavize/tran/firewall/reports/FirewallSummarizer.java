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

package com.metavize.tran.firewall.reports;

import java.sql.*;

import com.metavize.mvvm.reporting.BaseSummarizer;
import com.metavize.mvvm.reporting.Util;

import org.apache.log4j.Logger;

public class FirewallSummarizer extends BaseSummarizer {

    private static final Logger logger = Logger.getLogger(FirewallSummarizer.class);

    public FirewallSummarizer() { }

    public String getSummaryHtml(Connection conn, Timestamp startDate, Timestamp endDate)
    {
	long totalSessionsExamined = 0l;
	long totalSessionsBlocked = 0l;
	long totalSessionsBlockedByRule = 0l;
	long totalSessionsBlockedByDefault = 0l;
	long totalSessionsPassed = 0l;
	long totalSessionsPassedByRule = 0l;
	long totalSessionsPassedByDefault = 0l;
	long tcpSessionsBlocked = 0l;
	long tcpSessionsPassed = 0l;
	long udpSessionsBlocked = 0l;
	long udpSessionsPassed = 0l;
	long pingSessionsBlocked = 0l;
	long pingSessionsPassed = 0l;

		
        try {
            String sql;
	    PreparedStatement ps;
	    ResultSet rs;

	    sql = "select count(*) from tr_firewall_evt evt join firewall_rule using (rule_id) join pl_endp using (session_id) where evt.time_stamp >= ? and evt.time_stamp < ? and is_traffic_blocker = 't' and proto = 6";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            tcpSessionsBlocked = rs.getLong(1);
            rs.close();
            ps.close();

	    sql = "select count(*) from tr_firewall_evt evt join firewall_rule using (rule_id) join pl_endp using (session_id) where evt.time_stamp >= ? and evt.time_stamp < ? and is_traffic_blocker = 'f' and proto = 6";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            tcpSessionsPassed = rs.getLong(1);
            rs.close();
            ps.close();


	    sql = "select count(*) from tr_firewall_evt evt join firewall_rule using (rule_id) join pl_endp using (session_id) where evt.time_stamp >= ? and evt.time_stamp < ? and is_traffic_blocker = 't' and proto = 17";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            udpSessionsBlocked = rs.getLong(1);
            rs.close();
            ps.close();

	    sql = "select count(*) from tr_firewall_evt evt join firewall_rule using (rule_id) join pl_endp using (session_id) where evt.time_stamp >= ? and evt.time_stamp < ? and is_traffic_blocker = 'f' and proto = 17";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            udpSessionsPassed = rs.getLong(1);
            rs.close();
            ps.close();


	    sql = "select count(*) from tr_firewall_evt evt join firewall_rule using (rule_id) join pl_endp using (session_id) where evt.time_stamp >= ? and evt.time_stamp < ? and is_traffic_blocker = 't' and proto = 1";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            pingSessionsBlocked = rs.getLong(1);
            rs.close();
            ps.close();

	    sql = "select count(*) from tr_firewall_evt evt join firewall_rule using (rule_id) join pl_endp using (session_id) where evt.time_stamp >= ? and evt.time_stamp < ? and is_traffic_blocker = 'f' and proto = 1";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            pingSessionsPassed = rs.getLong(1);
            rs.close();
            ps.close();


        } catch (SQLException exn) {
            logger.warn("could not summarize", exn);
        }
	
        addEntry("Total sessions examined", Util.trimNumber(" XXXX",totalSessionsExamined));
        addEntry("&nbsp;&nbsp;&nbsp;Total blocked", Util.trimNumber(" XXXX",totalSessionsBlocked));
        addEntry("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Blocked by rule", Util.trimNumber(" XXXX",totalSessionsBlockedByRule));
        addEntry("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Blocked by default", Util.trimNumber(" XXXX",totalSessionsBlockedByDefault));
        addEntry("&nbsp;&nbsp;&nbsp;Total passed", Util.trimNumber(" XXXX",totalSessionsPassed));
        addEntry("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Passed by rule", Util.trimNumber(" XXXX",totalSessionsPassedByRule));
        addEntry("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Passed by default", Util.trimNumber(" XXXX",totalSessionsPassedByDefault));


	long trafficSessionsTotal = tcpSessionsBlocked + tcpSessionsPassed
	    + udpSessionsBlocked + udpSessionsPassed
	    + pingSessionsBlocked + pingSessionsPassed;

        addEntry("TCP", "");
        addEntry("&nbsp;&nbsp;&nbsp;Blocked", Long.toString(tcpSessionsBlocked) + " (" + Util.percentNumber(tcpSessionsBlocked, trafficSessionsTotal) + ")");
        addEntry("&nbsp;&nbsp;&nbsp;Passed", Long.toString(tcpSessionsPassed) + " (" + Util.percentNumber(tcpSessionsPassed, trafficSessionsTotal) + ")");
        addEntry("UDP", "");
        addEntry("&nbsp;&nbsp;&nbsp;Blocked", Long.toString(udpSessionsBlocked) + " (" + Util.percentNumber(udpSessionsBlocked, trafficSessionsTotal) + ")");
        addEntry("&nbsp;&nbsp;&nbsp;Passed", Long.toString(udpSessionsPassed) + " (" + Util.percentNumber(udpSessionsPassed, trafficSessionsTotal) + ")");
        addEntry("PING", "");
        addEntry("&nbsp;&nbsp;&nbsp;Blocked", Long.toString(pingSessionsBlocked) + " (" + Util.percentNumber(pingSessionsBlocked, trafficSessionsTotal) + ")");
        addEntry("&nbsp;&nbsp;&nbsp;Passed", Long.toString(pingSessionsPassed) + " (" + Util.percentNumber(pingSessionsPassed, trafficSessionsTotal) + ")");




        // XXXX
        String tranName = "Firewall";

        return summarizeEntries(tranName);
    }
}

