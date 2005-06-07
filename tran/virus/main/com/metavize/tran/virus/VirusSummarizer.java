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

package com.metavize.tran.virus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import com.metavize.mvvm.reporting.BaseSummarizer;
import org.apache.log4j.Logger;

public class VirusSummarizer extends BaseSummarizer {

    private static final Logger logger = Logger.getLogger(VirusSummarizer.class);

    public VirusSummarizer() { }

    public String getSummaryHtml(Connection conn, Timestamp startDate,
                                 Timestamp endDate)
    {
        int httpScanned = 0;
        int httpBlocked = 0;
        int ftpScanned = 0;
        int ftpBlocked = 0;

        try {
            String sql = "SELECT count(*) FROM tr_virus_evt_http WHERE time_stamp >= ? AND time_stamp < ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            ResultSet rs = ps.executeQuery();
            rs.first();
            httpScanned = rs.getInt(1);
            rs.close();
            ps.close();

            sql = "SELECT count(*) FROM tr_virus_evt_http WHERE time_stamp >= ? AND time_stamp < ? AND clean = false";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            httpBlocked = rs.getInt(1);
            rs.close();
            ps.close();


            sql = "SELECT count(*) FROM tr_virus_evt WHERE time_stamp >= ? AND time_stamp < ?";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            ftpScanned = rs.getInt(1);
            rs.close();
            ps.close();

            sql = "SELECT count(*) FROM tr_virus_evt WHERE time_stamp >= ? AND time_stamp < ? AND clean = false";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            ftpBlocked = rs.getInt(1);
            rs.close();
            ps.close();


            rs.close();
            ps.close();

        } catch (SQLException exn) {
            logger.warn("could not summarize", exn);
        }

        addEntry("Number of scanned HTTP downloads", httpScanned);
        addEntry("Number of infected/blocked HTTP downloads", httpBlocked);
        addEntry("Number of scanned FTP downloads", ftpScanned);
        addEntry("Number of infected/blocked HTTP downloads", ftpBlocked);

        // XXXX
        String tranName = "Virus";

        return summarizeEntries(tranName);
    }
}
