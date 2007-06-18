/*
 * $HeadURL$
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

package com.untangle.node.openvpn.reports;

import java.sql.*;

import com.untangle.uvm.reporting.BaseSummarizer;
import com.untangle.uvm.reporting.Util;
import org.apache.log4j.Logger;

public class VpnSummarizer extends BaseSummarizer {

    private final Logger logger = Logger.getLogger(getClass());

    public VpnSummarizer() { }

    public String getSummaryHtml(Connection conn, Timestamp startDate, Timestamp endDate)
    {
        int clientsLoggedIn = 0; // # of (successful) client logins
        int clientsDistributed = 0; // # of clients that have been distributed

        try {
            String sql;
            PreparedStatement ps;
            ResultSet rs;

            sql = "SELECT COUNT(*) FROM n_openvpn_connect_evt WHERE start_time >= ? AND start_time < ?";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            clientsLoggedIn = rs.getInt(1);
            rs.close();
            ps.close();

            sql = "SELECT COUNT(*) FROM n_openvpn_distr_evt WHERE time_stamp >= ? AND time_stamp < ?";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            clientsDistributed = rs.getInt(1);
            rs.close();
            ps.close();

        } catch (SQLException exn) {
            logger.warn("could not summarize", exn);
        }

        addEntry("User logins", Util.trimNumber("",clientsLoggedIn));
        addEntry("Client distributions", Util.trimNumber("",clientsDistributed));

        // XXXX
        String tranName = "OpenVpn";

        return summarizeEntries(tranName);
    }
}
