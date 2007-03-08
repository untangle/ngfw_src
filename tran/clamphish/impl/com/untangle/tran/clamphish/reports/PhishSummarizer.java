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

package com.untangle.tran.clamphish.reports;

import java.sql.*;

import com.untangle.mvvm.reporting.BaseSummarizer;
import com.untangle.mvvm.reporting.Util;
import org.apache.log4j.Logger;

public class PhishSummarizer extends BaseSummarizer {
    private final Logger logger = Logger.getLogger(getClass());

    public PhishSummarizer() { }

    public String getSummaryHtml(Connection conn, Timestamp startDate, Timestamp endDate)
    {
        // XXX shouldn't have this constant here.
        String spamVendor = (String) extraParams.get("spamVendor");

        logger.info("Identity Theft Blocker Vendor: " + spamVendor);

        long smtpScanned = 0l;
        long smtpMarked = 0l;
        long smtpBlocked = 0l;
        long smtpPassed = 0l;
        long smtpQuarantined = 0l;
        long popimapScanned = 0l;
        long popimapMarked = 0l;
        long popimapPassed = 0l;

        long httpBlocked = 0l;

        try {
            String sql;
            PreparedStatement ps;
            ResultSet rs;

            sql = "SELECT COUNT(*) FROM tr_spam_evt_smtp WHERE time_stamp >= ? AND time_stamp < ? AND vendor_name = ?";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            ps.setString(3, spamVendor);
            rs = ps.executeQuery();
            rs.first();
            smtpScanned = rs.getLong(1);
            rs.close();
            ps.close();

            sql = "SELECT COUNT(*) FROM tr_spam_evt_smtp WHERE time_stamp >= ? AND time_stamp < ? AND vendor_name = ? AND is_spam AND action = 'M'";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            ps.setString(3, spamVendor);
            rs = ps.executeQuery();
            rs.first();
            smtpMarked = rs.getLong(1);
            rs.close();
            ps.close();

            sql = "SELECT COUNT(*) FROM tr_spam_evt_smtp WHERE time_stamp >= ? AND time_stamp < ? AND vendor_name = ? AND is_spam AND action IN ('P','S','Z')";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            ps.setString(3, spamVendor);
            rs = ps.executeQuery();
            rs.first();
            smtpPassed = rs.getLong(1);
            rs.close();
            ps.close();

            sql = "SELECT COUNT(*) FROM tr_spam_evt_smtp WHERE time_stamp >= ? AND time_stamp < ? AND vendor_name = ? AND is_spam AND action = 'B'";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            ps.setString(3, spamVendor);
            rs = ps.executeQuery();
            rs.first();
            smtpBlocked = rs.getLong(1);
            rs.close();
            ps.close();

            sql = "SELECT COUNT(*) FROM tr_spam_evt_smtp WHERE time_stamp >= ? AND time_stamp < ? AND vendor_name = ? AND is_spam AND action = 'Q'";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            ps.setString(3, spamVendor);
            rs = ps.executeQuery();
            rs.first();
            smtpQuarantined = rs.getLong(1);
            rs.close();
            ps.close();

            sql = "SELECT COUNT(*) FROM tr_spam_evt WHERE time_stamp >= ? AND time_stamp < ? AND vendor_name = ?";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            ps.setString(3, spamVendor);
            rs = ps.executeQuery();
            rs.first();
            popimapScanned = rs.getLong(1);
            rs.close();
            ps.close();

            sql = "SELECT COUNT(*) FROM tr_spam_evt WHERE time_stamp >= ? AND time_stamp < ? AND vendor_name = ? AND is_spam AND action IN ('P','S','Z')";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            ps.setString(3, spamVendor);
            rs = ps.executeQuery();
            rs.first();
            popimapPassed = rs.getLong(1);
            rs.close();
            ps.close();

            sql = "SELECT COUNT(*) FROM tr_spam_evt WHERE time_stamp >= ? AND time_stamp < ? AND vendor_name = ? AND is_spam AND action = 'M'";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            ps.setString(3, spamVendor);
            rs = ps.executeQuery();
            rs.first();
            popimapMarked = rs.getLong(1);
            rs.close();
            ps.close();

            sql = "SELECT COUNT(*) FROM tr_phishhttp_evt WHERE time_stamp >= ? AND time_stamp < ? AND action = 'B'";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            httpBlocked = rs.getLong(1);
            rs.close();
            ps.close();

        } catch (SQLException exn) {
            logger.warn("could not summarize", exn);
        }

        addEntry("Scanned emails (SMTP)", Util.trimNumber("",smtpScanned));
        addEntry("&nbsp;&nbsp;&nbsp;Phish & Quarantined", Util.trimNumber("",smtpQuarantined), Util.percentNumber(smtpQuarantined, smtpScanned));
        addEntry("&nbsp;&nbsp;&nbsp;Phish & Blocked", Util.trimNumber("",smtpBlocked), Util.percentNumber(smtpBlocked, smtpScanned));
        addEntry("&nbsp;&nbsp;&nbsp;Phish & Marked", Util.trimNumber("",smtpMarked), Util.percentNumber(smtpMarked, smtpScanned));
        addEntry("&nbsp;&nbsp;&nbsp;Phish & Passed", Util.trimNumber("",smtpPassed), Util.percentNumber(smtpPassed, smtpScanned));
        long totalSpam = smtpQuarantined + smtpBlocked + smtpMarked + smtpPassed;
        addEntry("&nbsp;&nbsp;&nbsp;Clean & Passed", Util.trimNumber("",smtpScanned-totalSpam), Util.percentNumber(smtpScanned-totalSpam, smtpScanned));

        addEntry("&nbsp;","&nbsp;");

        addEntry("Scanned emails (POP/IMAP)", Util.trimNumber("",popimapScanned));
        addEntry("&nbsp;&nbsp;&nbsp;Phish & Marked", Util.trimNumber("",popimapMarked), Util.percentNumber(popimapMarked, popimapScanned));
        addEntry("&nbsp;&nbsp;&nbsp;Phish & Passed", Util.trimNumber("",popimapPassed), Util.percentNumber(popimapPassed, popimapScanned));
        totalSpam = popimapMarked + popimapPassed;
        addEntry("&nbsp;&nbsp;&nbsp;Clean & Passed", Util.trimNumber("",popimapScanned-totalSpam), Util.percentNumber(popimapScanned-totalSpam, popimapScanned));

        addEntry("&nbsp;","&nbsp;");

        addEntry("Web logged violations: Phish & Blocked", Util.trimNumber("",httpBlocked));

        String tranName = "Identity Theft Blocker";

        return summarizeEntries(tranName);
    }
}
