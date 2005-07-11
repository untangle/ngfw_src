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


	    sql = " SELECT sum(SESSIONS)," +
                  " sum(TCP_BLOCK_DEFAULT),  sum(TCP_BLOCK_RULE)," +
                  " sum(TCP_PASS_DEFAULT),   sum(TCP_PASS_RULE)," +
                  " sum(UDP_BLOCK_DEFAULT),  sum(UDP_BLOCK_RULE)," +
                  " sum(UDP_PASS_DEFAULT),   sum(UDP_PASS_RULE)," +
                  " sum(ICMP_BLOCK_DEFAULT), sum(ICMP_BLOCK_RULE)," +
                  " sum(ICMP_PASS_DEFAULT),  sum(ICMP_PASS_RULE)" +
                  " FROM tr_firewall_statistic_evt evt where evt.time_stamp >= ? and evt.time_stamp < ?";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
	    totalSessionsExamined = rs.getLong(1);
            tcpSessionsBlockedDefault = rs.getLong(2);
            tcpSessionsBlockedRule = rs.getLong(3);
            tcpSessionsPassedDefault = rs.getLong(4);
            tcpSessionsPassedRule = rs.getLong(5);
            udpSessionsBlockedDefault = rs.getLong(6);
            udpSessionsBlockedRule = rs.getLong(7);
            udpSessionsPassedDefault = rs.getLong(8);
            udpSessionsPassedRule = rs.getLong(9);
            pingSessionsBlockedDefault = rs.getLong(10);
            pingSessionsBlockedRule = rs.getLong(11);
            pingSessionsPassedDefault = rs.getLong(12);
            pingSessionsPassedRule = rs.getLong(13);
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
	
        addEntry("Total sessions examined", Util.trimNumber("",totalSessionsExamined));
        addEntry("&nbsp;&nbsp;&nbsp;Total blocked", Util.trimNumber("",totalSessionsBlocked));
        addEntry("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Blocked by rule", Util.trimNumber("",totalSessionsBlockedRule));
        addEntry("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Blocked by default", Util.trimNumber("",totalSessionsBlockedDefault));
        addEntry("&nbsp;&nbsp;&nbsp;Total passed", Util.trimNumber("",totalSessionsPassed));
        addEntry("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Passed by rule", Util.trimNumber("",totalSessionsPassedRule));
        addEntry("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Passed by default", Util.trimNumber("",totalSessionsPassedDefault));


        addEntry("&nbsp;", "&nbsp;");

        addEntry("TCP", "");
        addEntry("&nbsp;&nbsp;&nbsp;Blocked", Long.toString(tcpSessionsBlockedRule+tcpSessionsBlockedDefault) 
		 + " (" + Util.percentNumber(tcpSessionsBlockedRule+tcpSessionsBlockedDefault, totalSessionsExamined) + ")");
        addEntry("&nbsp;&nbsp;&nbsp;Passed", Long.toString(tcpSessionsPassedRule+tcpSessionsPassedDefault)
		 + " (" + Util.percentNumber(tcpSessionsPassedRule+tcpSessionsPassedDefault, totalSessionsExamined) + ")");
        addEntry("UDP", "");
        addEntry("&nbsp;&nbsp;&nbsp;Blocked", Long.toString(udpSessionsBlockedRule+udpSessionsBlockedDefault)
		 + " (" + Util.percentNumber(udpSessionsBlockedRule+udpSessionsBlockedDefault, totalSessionsExamined) + ")");
        addEntry("&nbsp;&nbsp;&nbsp;Passed", Long.toString(udpSessionsPassedRule+udpSessionsPassedDefault)
		 + " (" + Util.percentNumber(udpSessionsPassedRule+udpSessionsPassedDefault, totalSessionsExamined) + ")");
        addEntry("PING", "");
        addEntry("&nbsp;&nbsp;&nbsp;Blocked", Long.toString(pingSessionsBlockedRule+pingSessionsBlockedDefault)
		 + " (" + Util.percentNumber(pingSessionsBlockedRule+pingSessionsBlockedDefault, totalSessionsExamined) + ")");
        addEntry("&nbsp;&nbsp;&nbsp;Passed", Long.toString(pingSessionsPassedRule+pingSessionsPassedDefault)
		 + " (" + Util.percentNumber(pingSessionsPassedRule+pingSessionsPassedDefault, totalSessionsExamined) + ")");




        // XXXX
        String tranName = "Firewall";

        return summarizeEntries(tranName);
    }
}

