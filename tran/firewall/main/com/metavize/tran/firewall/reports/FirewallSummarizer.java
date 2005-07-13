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


	    sql = " SELECT sum(TCP_BLOCK_DEFAULT),  sum(TCP_BLOCK_RULE)," +
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

        addEntry("Total sessions examined", Util.trimNumber("",totalSessionsExamined));

	addEntry("&nbsp;","&nbsp;");

        addEntry("Total sessions blocked", Util.trimNumber("",totalSessionsBlocked) 
		 + " (" + Util.percentNumber(totalSessionsBlocked, totalSessionsExamined) + ")");
        addEntry("&nbsp;&nbsp;&nbsp;Blocked by rule", Util.trimNumber("",totalSessionsBlockedRule) 
		 + " (" + Util.percentNumber(totalSessionsBlockedRule, totalSessionsExamined) + ")");
        addEntry("&nbsp;&nbsp;&nbsp;Blocked by default", Util.trimNumber("",totalSessionsBlockedDefault) 
		 + " (" + Util.percentNumber(totalSessionsBlockedDefault, totalSessionsExamined) + ")");
        addEntry("Total sessions passed", Util.trimNumber("",totalSessionsPassed) 
		 + " (" + Util.percentNumber(totalSessionsPassed, totalSessionsExamined) + ")");
        addEntry("&nbsp;&nbsp;&nbsp;Passed by rule", Util.trimNumber("",totalSessionsPassedRule) 
		 + " (" + Util.percentNumber(totalSessionsPassedRule, totalSessionsExamined) + ")");
        addEntry("&nbsp;&nbsp;&nbsp;Passed by default", Util.trimNumber("",totalSessionsPassedDefault) 
		 + " (" + Util.percentNumber(totalSessionsPassedDefault, totalSessionsExamined) + ")");

        addEntry("&nbsp;", "&nbsp;");
	
        addEntry("TCP sessions",  Long.toString(tcpSessionsBlockedRule+tcpSessionsBlockedDefault+tcpSessionsPassedRule+tcpSessionsPassedDefault) 
		 + " (" + Util.percentNumber(tcpSessionsBlockedRule+tcpSessionsBlockedDefault+tcpSessionsPassedRule+tcpSessionsPassedDefault, totalSessionsExamined) + ")");
        addEntry("&nbsp;&nbsp;&nbsp;Blocked", Long.toString(tcpSessionsBlockedRule+tcpSessionsBlockedDefault) 
		 + " (" + Util.percentNumber(tcpSessionsBlockedRule+tcpSessionsBlockedDefault, totalSessionsExamined) + ")");
        addEntry("&nbsp;&nbsp;&nbsp;Passed", Long.toString(tcpSessionsPassedRule+tcpSessionsPassedDefault)
		 + " (" + Util.percentNumber(tcpSessionsPassedRule+tcpSessionsPassedDefault, totalSessionsExamined) + ")");
	addEntry("UDP sessions",  Long.toString(udpSessionsBlockedRule+udpSessionsBlockedDefault+udpSessionsPassedRule+udpSessionsPassedDefault) 
		 + " (" + Util.percentNumber(udpSessionsBlockedRule+udpSessionsBlockedDefault+udpSessionsPassedRule+udpSessionsPassedDefault, totalSessionsExamined) + ")");
        addEntry("&nbsp;&nbsp;&nbsp;Blocked", Long.toString(udpSessionsBlockedRule+udpSessionsBlockedDefault)
		 + " (" + Util.percentNumber(udpSessionsBlockedRule+udpSessionsBlockedDefault, totalSessionsExamined) + ")");
        addEntry("&nbsp;&nbsp;&nbsp;Passed", Long.toString(udpSessionsPassedRule+udpSessionsPassedDefault)
		 + " (" + Util.percentNumber(udpSessionsPassedRule+udpSessionsPassedDefault, totalSessionsExamined) + ")");
	addEntry("PING sessions",  Long.toString(pingSessionsBlockedRule+pingSessionsBlockedDefault+pingSessionsPassedRule+pingSessionsPassedDefault)  + " (" + Util.percentNumber(pingSessionsBlockedRule+pingSessionsBlockedDefault+pingSessionsPassedRule+pingSessionsPassedDefault, totalSessionsExamined) + ")");
        addEntry("&nbsp;&nbsp;&nbsp;Blocked", Long.toString(pingSessionsBlockedRule+pingSessionsBlockedDefault)
		 + " (" + Util.percentNumber(pingSessionsBlockedRule+pingSessionsBlockedDefault, totalSessionsExamined) + ")");
        addEntry("&nbsp;&nbsp;&nbsp;Passed", Long.toString(pingSessionsPassedRule+pingSessionsPassedDefault)
		 + " (" + Util.percentNumber(pingSessionsPassedRule+pingSessionsPassedDefault, totalSessionsExamined) + ")");




        // XXXX
        String tranName = "Firewall";

        return summarizeEntries(tranName);
    }
}

