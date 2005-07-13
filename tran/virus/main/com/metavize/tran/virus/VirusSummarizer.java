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
import com.metavize.mvvm.reporting.Util;
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
	int emailScanned = 0;
	int emailBlocked = 0;

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

	    /////// INSERT SCANNED QUERY HERE

	    /////// INSERT BLOCKED QUERY HERE


            rs.close();
            ps.close();

        } catch (SQLException exn) {
            logger.warn("could not summarize", exn);
        }

        addEntry("Scanned Web downloads", Util.trimNumber("",httpScanned));
        addEntry("&nbsp;&nbsp;&nbsp;Blocked (infected)", Util.trimNumber("",httpBlocked));
        addEntry("&nbsp;&nbsp;&nbsp;Passed", Util.trimNumber("",httpScanned - httpBlocked));
        addEntry("Scanned FTP downloads", Util.trimNumber("",ftpScanned));
        addEntry("&nbsp;&nbsp;&nbsp;Blocked (infected)", Util.trimNumber("",ftpBlocked));
        addEntry("&nbsp;&nbsp;&nbsp;Passed", Util.trimNumber("",ftpScanned - ftpBlocked));
        addEntry("Scanned Email downloads", Util.trimNumber("XXXX",ftpScanned));
        addEntry("&nbsp;&nbsp;&nbsp;Blocked (infected)", Util.trimNumber("XXXX",emailBlocked));
        addEntry("&nbsp;&nbsp;&nbsp;Passed", Util.trimNumber("XXXX",emailScanned - emailBlocked));

        // XXXX
        String tranName = "Virus";

        return summarizeEntries(tranName);
    }
}
