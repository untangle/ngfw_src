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

package com.metavize.tran.nat.reports;

import java.sql.*;

import com.metavize.mvvm.reporting.BaseSummarizer;
import com.metavize.mvvm.reporting.Util;

import org.apache.log4j.Logger;

public class NatSummarizer extends BaseSummarizer {

    private static final Logger logger = Logger.getLogger(NatSummarizer.class);

    public NatSummarizer() { }

    public String getSummaryHtml(Connection conn, Timestamp startDate, Timestamp endDate)
    {

	long totalOutboundSessionsCreated = 0l;
	long totalAddressAssignments = 0l;
	long dhcpAddressAssignments = 0l;
	long staticAddressAssignments = 0l;
	long totalRedirections = 0l;
	long tcpRedirections = 0l;
	long tcpInboundRedirections = 0l;
	long tcpOutboundRedirections = 0l;
	long udpRedirections = 0l;
	long udpInboundRedirections = 0l;
	long udpOutboundRedirections = 0l;
	long pingRedirections = 0l;
	long pingInboundRedirections = 0l;
	long pingOutboundRedirections = 0l;
	long dmzRedirections = 0l;
	long dnsForwards = 0l;

	/*
        try {
            String sql;
	    PreparedStatement ps;
	    ResultSet rs;
	    
	    sql = "select count(*) from tr_protofilter_evt where time_stamp >= ? and time_stamp < ?";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            logCount = rs.getInt(1);
            rs.close();
            ps.close();

            sql = "select count(*) from tr_protofilter_evt where time_stamp >= ? and time_stamp < ? and blocked = 't'";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            blockCount = rs.getInt(1);
            rs.close();
            ps.close();
	    

        } catch (SQLException exn) {
            logger.warn("could not summarize", exn);
        }

	*/


        addEntry("Total outbound NAT sessions created", Util.trimNumber(" XXXX",totalOutboundSessionsCreated));
        addEntry("Total IP address assignments", Util.trimNumber(" XXXX",totalAddressAssignments));
        addEntry("&nbsp;&nbsp;&nbsp;Dynamically via DHCP", Util.trimNumber(" XXXX",dhcpAddressAssignments));
        addEntry("&nbsp;&nbsp;&nbsp;Statically", Util.trimNumber(" XXXX",staticAddressAssignments));
        addEntry("Total redirections", Util.trimNumber(" XXXX",totalRedirections) );
        addEntry("&nbsp;&nbsp;&nbsp;TCP", Util.trimNumber(" XXXX",tcpRedirections) );
        addEntry("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Inbound", Util.trimNumber(" XXXX",tcpInboundRedirections) );
        addEntry("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Outbound", Util.trimNumber(" XXXX",tcpOutboundRedirections) );
        addEntry("&nbsp;&nbsp;&nbsp;UDP", Util.trimNumber(" XXXX",udpRedirections) );
        addEntry("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Inbound", Util.trimNumber(" XXXX",udpInboundRedirections) );
        addEntry("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Outbound", Util.trimNumber(" XXXX",udpOutboundRedirections) );
        addEntry("&nbsp;&nbsp;&nbsp;PING", Util.trimNumber(" XXXX",pingRedirections) );
        addEntry("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Inbound", Util.trimNumber(" XXXX",pingInboundRedirections) );
        addEntry("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Outbound", Util.trimNumber(" XXXX",pingOutboundRedirections) );
        addEntry("DMZ inbound redirections", Util.trimNumber(" XXXX",dmzRedirections) );
        addEntry("Total DNS forwards", Util.trimNumber(" XXXX",dnsForwards) );



        // XXXX
        String tranName = "Network Sharing";

        return summarizeEntries(tranName);
    }
}

