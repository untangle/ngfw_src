/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: EmailSummarizer.java,v 1.3 2005/03/15 04:33:05 cng Exp $
 */

package com.metavize.tran.email;

import java.sql.*;
import org.apache.log4j.Logger;

import com.metavize.mvvm.reporting.BaseSummarizer;
import com.metavize.mvvm.reporting.ReportSummarizer;

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
        int blockSzLimitCnt = 0;

        try {
            String sql = "select count(*) from tr_email_spam_evt where time_stamp >= ? and time_stamp < ? and (action != 'P')";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            ResultSet rs = ps.executeQuery();
            rs.first();
            blockSpamCnt = rs.getInt(1);
            rs.close();
            ps.close();

            sql = "select count(*) from tr_email_virus_evt where time_stamp >= ? and time_stamp < ? and (action != 'P' and action != 'C')";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            blockVirusCnt = rs.getInt(1);
            rs.close();
            ps.close();

            sql = "select count(*) from tr_email_virus_evt where time_stamp >= ? and time_stamp < ? and action = 'C'";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first(); 
            replaceVirusCnt = rs.getInt(1);
            rs.close();
            ps.close();

            sql = "select count(*) from tr_email_custom_evt where time_stamp >= ? and time_stamp < ? and action = 'B'";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            blockCustomCnt = rs.getInt(1);
            rs.close();
            ps.close();

            //sql = "select count(*) from tr_email_custom_evt where time_stamp >= ? and time_stamp < ? and action = 'E'";
            //ps = conn.prepareStatement(sql);
            //ps.setTimestamp(1, startDate);
            //ps.setTimestamp(2, endDate);
            //rs = ps.executeQuery();
            //rs.first();
            //exchangeCustomCnt = rs.getInt(1);
            //rs.close();
            //ps.close();

            sql = "select count(*) from tr_email_szlimit_evt where time_stamp >= ? and time_stamp < ?";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            blockSzLimitCnt = rs.getInt(1);
            rs.close();
            ps.close();

            sql = "select count(*) from tr_email_custom_evt where time_stamp >= ? and time_stamp < ? and action = 'P'";
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

        addEntry("Number of blocked Spam messages", blockSpamCnt);
        addEntry("Number of blocked Virus messages", blockVirusCnt);
        addEntry("Number of modified Virus messages", replaceVirusCnt);
        addEntry("Number of blocked Custom rule messages", blockCustomCnt);
        //addEntry("Number of modified Custom rule messages", exchangeCustomCnt);
        addEntry("Number of passed messages", passCnt);
        addEntry("Number of blocked Size Limit messages", blockSzLimitCnt);
        int totalCnt = blockSpamCnt + blockVirusCnt + replaceVirusCnt + blockCustomCnt + passCnt + blockSzLimitCnt;
        addEntry("Total number of scanned messages", totalCnt);

        // XXXX
        String tranName = "eMail SpamGuard";

        return summarizeEntries(tranName);
    }
}
