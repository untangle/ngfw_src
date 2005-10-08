/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: IDSSummarizer.java 2173 2005-08-18 02:22:16Z inieves $
 */

package com.metavize.tran.ids.reports;

import java.sql.*;

import com.metavize.mvvm.reporting.BaseSummarizer;
import com.metavize.mvvm.reporting.Util;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;

public class IDSSummarizer extends BaseSummarizer {

    private static final Logger log = Logger.getLogger(IDSSummarizer.class);

	static {
		log.setLevel(Level.ALL);
	}
	
    public IDSSummarizer() { }

    public String getSummaryHtml(Connection conn, Timestamp startDate, Timestamp endDate)
    {
        long scannedEvtCount 	= 0;
	long passedEvtCount 	= 0;
	long blockedEvtCount	= 0;
	
        try {
	    String sql;
	    PreparedStatement ps;
	    ResultSet rs;
	    
            sql = "SELECT SUM(ids_scanned), SUM(ids_passed), SUM(ids_blocked) FROM tr_ids_statistic_evt WHERE time_stamp >= ? AND time_stamp < ?";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
	    
            scannedEvtCount = rs.getLong(1);
	    passedEvtCount = rs.getLong(2);
	    blockedEvtCount = rs.getLong(3);
	    
	    rs.close();
            ps.close();
	    
        } catch (SQLException exn) {
            log.warn("could not summarize", exn);
        }
	
        long matchedEvtCount = blockedEvtCount + passedEvtCount;
	addEntry("Total Scan Events", Util.trimNumber("",scannedEvtCount));
        addEntry("&nbsp;&nbsp;&nbsp;Matched &amp; Passed", Util.trimNumber("",passedEvtCount), Util.percentNumber(passedEvtCount,scannedEvtCount));
        addEntry("&nbsp;&nbsp;&nbsp;Matched &amp; Blocked", Util.trimNumber("",blockedEvtCount), Util.percentNumber(blockedEvtCount,scannedEvtCount));
        addEntry("&nbsp;&nbsp;&nbsp;Unmatched", Util.trimNumber("",scannedEvtCount - matchedEvtCount), Util.percentNumber(scannedEvtCount-matchedEvtCount,scannedEvtCount));	
        // XXXX
        String tranName = "Intrusion Prevention";
	
        return summarizeEntries(tranName);
    }
}
