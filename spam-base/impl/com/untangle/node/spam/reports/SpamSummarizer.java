/*
 * $HeadURL:$
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

package com.untangle.node.spam.reports;

import java.sql.*;

import com.untangle.uvm.reporting.BaseSummarizer;
import com.untangle.uvm.reporting.Util;
import org.apache.log4j.Logger;

public class SpamSummarizer extends BaseSummarizer {

    private final Logger logger = Logger.getLogger(getClass());

    public SpamSummarizer() { }

    public String getSummaryHtml(Connection conn, Timestamp startDate, Timestamp endDate)
    {
        // XXX shouldn't have this constant here.
        String spamVendor = (String) extraParams.get("spamVendor");

        logger.info("Spam Vendor: " + spamVendor);

        int smtpScanned = 0;
        int smtpMarked = 0;
        int smtpBlocked = 0;
        int smtpPassed = 0;
        int smtpQuarantined = 0;
        int popimapScanned = 0;
        int popimapMarked = 0;
        int popimapPassed = 0;
        int smtpRBLRejected = 0;

        try {
            String sql;
            PreparedStatement ps;
            ResultSet rs;

            sql = "SELECT COUNT(*) FROM n_spam_evt_smtp WHERE time_stamp >= ? AND time_stamp < ? AND vendor_name = ?";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            ps.setString(3, spamVendor);
            rs = ps.executeQuery();
            rs.first();
            smtpScanned = rs.getInt(1);
            rs.close();
            ps.close();

            sql = "SELECT COUNT(*) FROM n_spam_evt_smtp WHERE time_stamp >= ? AND time_stamp < ? AND vendor_name = ? AND is_spam AND action = 'M'";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            ps.setString(3, spamVendor);
            rs = ps.executeQuery();
            rs.first();
            smtpMarked = rs.getInt(1);
            rs.close();
            ps.close();

            sql = "SELECT COUNT(*) FROM n_spam_evt_smtp WHERE time_stamp >= ? AND time_stamp < ? AND vendor_name = ? AND is_spam AND action IN ('P','S','Z')";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            ps.setString(3, spamVendor);
            rs = ps.executeQuery();
            rs.first();
            smtpPassed = rs.getInt(1);
            rs.close();
            ps.close();

            sql = "SELECT COUNT(*) FROM n_spam_evt_smtp WHERE time_stamp >= ? AND time_stamp < ? AND vendor_name = ? AND is_spam AND action = 'B'";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            ps.setString(3, spamVendor);
            rs = ps.executeQuery();
            rs.first();
            smtpBlocked = rs.getInt(1);
            rs.close();
            ps.close();

            sql = "SELECT COUNT(*) FROM n_spam_evt_smtp WHERE time_stamp >= ? AND time_stamp < ? AND vendor_name = ? AND is_spam AND action = 'Q'";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            ps.setString(3, spamVendor);
            rs = ps.executeQuery();
            rs.first();
            smtpQuarantined = rs.getInt(1);
            rs.close();
            ps.close();

            sql = "SELECT COUNT(*) FROM n_spam_evt WHERE time_stamp >= ? AND time_stamp < ? AND vendor_name = ?";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            ps.setString(3, spamVendor);
            rs = ps.executeQuery();
            rs.first();
            popimapScanned = rs.getInt(1);
            rs.close();
            ps.close();

            sql = "SELECT COUNT(*) FROM n_spam_evt WHERE time_stamp >= ? AND time_stamp < ? AND vendor_name = ? AND is_spam AND action IN ('P','S','Z')";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            ps.setString(3, spamVendor);
            rs = ps.executeQuery();
            rs.first();
            popimapPassed = rs.getInt(1);
            rs.close();
            ps.close();

            sql = "SELECT COUNT(*) FROM n_spam_evt WHERE time_stamp >= ? AND time_stamp < ? AND vendor_name = ? AND is_spam AND action = 'M'";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            ps.setString(3, spamVendor);
            rs = ps.executeQuery();
            rs.first();
            popimapMarked = rs.getInt(1);
            rs.close();
            ps.close();

            sql = "SELECT COUNT(*) FROM n_spam_smtp_rbl_evt WHERE skipped = FALSE AND time_stamp >= ? AND time_stamp < ?";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            smtpRBLRejected = rs.getInt(1);
            rs.close();
            ps.close();

        } catch (SQLException exn) {
            logger.warn("could not summarize", exn);
        }

	// "Scanned" must include those we stopped even earlier.
	smtpScanned += smtpRBLRejected;
        addEntry("Scanned emails (SMTP)", Util.trimNumber("",smtpScanned));
	if (smtpRBLRejected > 0)
            addEntry("&nbsp;&nbsp;&nbsp;Spam connection rejected using DSNBLs", Util.trimNumber("",smtpRBLRejected), Util.percentNumber(smtpRBLRejected, smtpScanned));
	if (smtpQuarantined > 0)
            addEntry("&nbsp;&nbsp;&nbsp;Spam & Quarantined", Util.trimNumber("",smtpQuarantined), Util.percentNumber(smtpQuarantined, smtpScanned));
	if (smtpBlocked > 0)
            addEntry("&nbsp;&nbsp;&nbsp;Spam & Blocked", Util.trimNumber("",smtpBlocked), Util.percentNumber(smtpBlocked, smtpScanned));
	if (smtpMarked > 0)
            addEntry("&nbsp;&nbsp;&nbsp;Spam & Marked", Util.trimNumber("",smtpMarked), Util.percentNumber(smtpMarked, smtpScanned));
	if (smtpPassed > 0)
            addEntry("&nbsp;&nbsp;&nbsp;Spam & Passed", Util.trimNumber("",smtpPassed), Util.percentNumber(smtpPassed, smtpScanned));
        int totalSpam = smtpRBLRejected + smtpQuarantined + smtpBlocked + smtpMarked + smtpPassed;
        addEntry("&nbsp;&nbsp;&nbsp;Clean & Passed", Util.trimNumber("",smtpScanned-totalSpam), Util.percentNumber(smtpScanned-totalSpam, smtpScanned));

        addEntry("&nbsp;","&nbsp;");

        addEntry("Scanned emails (POP/IMAP)", Util.trimNumber("",popimapScanned));
	if (popimapMarked > 0)
            addEntry("&nbsp;&nbsp;&nbsp;Spam & Marked", Util.trimNumber("",popimapMarked), Util.percentNumber(popimapMarked, popimapScanned));
	if (popimapPassed > 0)
            addEntry("&nbsp;&nbsp;&nbsp;Spam & Passed", Util.trimNumber("",popimapPassed), Util.percentNumber(popimapPassed, popimapScanned));
        totalSpam = popimapMarked + popimapPassed;
        addEntry("&nbsp;&nbsp;&nbsp;Clean & Passed", Util.trimNumber("",popimapScanned-totalSpam), Util.percentNumber(popimapScanned-totalSpam, popimapScanned));

        // XXXX
        String tranName = "Spam Blocker";

        return summarizeEntries(tranName);
    }
}
