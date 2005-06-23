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

package com.metavize.tran.email;

import java.sql.*;
import org.apache.log4j.Logger;

import com.metavize.mvvm.reporting.BaseSummarizer;
import com.metavize.mvvm.reporting.ReportSummarizer;
import com.metavize.mvvm.reporting.Util;

public class EmailSummarizer extends BaseSummarizer {
    
    private static final Logger logger = Logger.getLogger(EmailSummarizer.class);

    public EmailSummarizer() { }

    public String getSummaryHtml(Connection conn, Timestamp startDate, Timestamp endDate)
    {
        int blockSpamCnt = 0;
        int blockVirusCnt = 0;
        int replaceVirusCnt = 0;
        int blockCustomCnt = 0;
        //int exchangeCustomCnt = 0;
        int passCnt = 0;
        int relaySzCnt = 0;

        try {
            String sql = "SELECT COUNT(*) FROM tr_email_spam_evt WHERE time_stamp >= ? AND time_stamp < ? AND (action != 'M')";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            ResultSet rs = ps.executeQuery();
            rs.first();
            blockSpamCnt = rs.getInt(1);
            rs.close();
            ps.close();

            sql = "SELECT COUNT(*) FROM tr_email_virus_evt WHERE time_stamp >= ? AND time_stamp < ? AND (action != 'P' AND action != 'C')";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            blockVirusCnt = rs.getInt(1);
            rs.close();
            ps.close();

            sql = "SELECT COUNT(*) FROM tr_email_virus_evt WHERE time_stamp >= ? AND time_stamp < ? AND action = 'C'";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first(); 
            replaceVirusCnt = rs.getInt(1);
            rs.close();
            ps.close();

            sql = "SELECT COUNT(*) FROM tr_email_custom_evt WHERE time_stamp >= ? AND time_stamp < ? AND action = 'B'";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            blockCustomCnt = rs.getInt(1);
            rs.close();
            ps.close();

            //sql = "SELECT COUNT(*) FROM tr_email_custom_evt WHERE time_stamp >= ? AND time_stamp < ? AND action = 'E'";
            //ps = conn.prepareStatement(sql);
            //ps.setTimestamp(1, startDate);
            //ps.setTimestamp(2, endDate);
            //rs = ps.executeQuery();
            //rs.first();
            //exchangeCustomCnt = rs.getInt(1);
            //rs.close();
            //ps.close();

            sql = "SELECT COUNT(*) FROM tr_email_szrelay_evt WHERE time_stamp >= ? AND time_stamp < ?";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            relaySzCnt = rs.getInt(1);
            rs.close();
            ps.close();

            sql = "SELECT COUNT(*) FROM tr_email_custom_evt WHERE time_stamp >= ? AND time_stamp < ? AND action = 'P'";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            passCnt = rs.getInt(1);
            rs.close();
            ps.close();

        } catch (SQLException exn) {
            logger.warn("could not summarize", exn);
        }

        int totalCnt = blockSpamCnt + blockVirusCnt + replaceVirusCnt + blockCustomCnt + passCnt;
        addEntry("Total scanned messages", Util.trimNumber("",totalCnt));
        addEntry("&nbsp;&nbsp;&nbsp;Blocked spam messages", Util.trimNumber("",blockSpamCnt));
        addEntry("&nbsp;&nbsp;&nbsp;Blocked virus messages", Util.trimNumber("",blockVirusCnt));
        addEntry("&nbsp;&nbsp;&nbsp;Modified virus messages", Util.trimNumber("",replaceVirusCnt));
        addEntry("&nbsp;&nbsp;&nbsp;Blocked custom rule messages", Util.trimNumber("",blockCustomCnt));
        //addEntry("Number of modified Custom rule messages", exchangeCustomCnt);
        addEntry("&nbsp;&nbsp;&nbsp;Passed messages", Util.trimNumber("",passCnt));
        addEntry("Total messages not processed", Util.trimNumber("",relaySzCnt));

        // XXXX
        String tranName = "eMail SpamGuard";

        return summarizeEntries(tranName);
    }
}
