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

package com.untangle.uvm.reporting.summary;

import java.sql.*;
import java.util.*;

import com.untangle.uvm.reporting.*;
import com.untangle.uvm.util.PortServiceNames;
import net.sf.jasperreports.engine.JRScriptletException;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardPieToolTipGenerator;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.PiePlot3D;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.ui.RectangleInsets;
import org.jfree.util.Rotation;


public class TrafficByPortGraph extends TopTenPieGraph
{
    // PUT ELSEWHERE XXX
    private static final short PROTO_TCP = 6;
    private static final short PROTO_UDP = 17;

    private String chartTitle;

    private boolean doOutgoingSessions;
    private boolean doIncomingSessions;
    private boolean countOutgoingBytes;
    private boolean countIncomingBytes;

    private boolean byPercentage;

    private String seriesTitle;

    public TrafficByPortGraph(String chartTitle, boolean byPercentage,
                              boolean doOutgoingSessions, boolean doIncomingSessions,
                              boolean countOutgoingBytes, boolean countIncomingBytes)
    {
        this.chartTitle = chartTitle;
        this.byPercentage = byPercentage;
        this.doOutgoingSessions = doOutgoingSessions;
        this.doIncomingSessions = doIncomingSessions;
        this.countOutgoingBytes = countOutgoingBytes;
        this.countIncomingBytes = countIncomingBytes;
    }

    protected JFreeChart doChart(Connection con) throws JRScriptletException, SQLException
    {
        DefaultPieDataset dataset = new DefaultPieDataset();

        System.out.println("Start Date: " + startDate + ", End Date: " + endDate);

        // Load up the datasets
        String sql = "SELECT proto, s_server_port, client_intf, count(*), sum(c2p_bytes), sum(p2s_bytes), sum(s2p_bytes), sum(p2c_bytes) FROM pl_endp endp JOIN pl_stats stats ON endp.event_id = stats.pl_endp_id WHERE ";
        if (!doIncomingSessions || !doOutgoingSessions)
            sql += "client_intf = ? AND ";
        sql += "endp.time_stamp <= ? and stats.time_stamp >= ? GROUP BY client_intf, proto, s_server_port";
        int sidx = 1;
        PreparedStatement stmt = con.prepareStatement(sql);
        if (doIncomingSessions && !doOutgoingSessions) {
            stmt.setShort(sidx++, (short)0);
        } else if (!doIncomingSessions && doOutgoingSessions) {
            stmt.setShort(sidx++, (short)1);
        }
        stmt.setTimestamp(sidx++, endDate);
        stmt.setTimestamp(sidx++, startDate);
        ResultSet rs = stmt.executeQuery();

        List<GEntry> byteCounts = new ArrayList<GEntry>();
        PortServiceNames psn = PortServiceNames.get();
        int count;
        for (count = 1; rs.next(); count++) {
            short proto = rs.getShort(1);
            int serverPort = rs.getInt(2);
            short clientIntf = rs.getShort(3);
            int sessionCount = rs.getInt(4);
            long c2pBytes = rs.getLong(5);
            long p2sBytes = rs.getLong(6);
            long s2pBytes = rs.getLong(7);
            long p2cBytes = rs.getLong(8);
            long byteCount = 0;
            if (countIncomingBytes) {
                if (clientIntf == 0)
                    byteCount += c2pBytes;
                else
                    byteCount += s2pBytes;
            }
            if (countOutgoingBytes) {
                if (clientIntf == 0)
                    byteCount += p2cBytes;
                else
                    byteCount += p2sBytes;
            }

            String portName = null;
            switch (proto) {
            case PROTO_TCP:
                portName = psn.getTCPServiceName(serverPort);
                if (portName == null)
                    portName = String.valueOf(serverPort) + "/tcp";
                break;
            case PROTO_UDP:
                portName = psn.getUDPServiceName(serverPort);
                if (portName == null)
                    portName = String.valueOf(serverPort) + "/udp";
                break;
            }
            byteCounts.add(new GEntry(portName, byteCount));
        }
        try { stmt.close(); } catch (SQLException x) { }

        // Post-process to sort the set, deal with the leftover, and produce percentages
        Collections.sort(byteCounts);
        if (byteCounts.size() > maxPieSlices - 1) {
            double leftoverByteCount = 0d;
            for (int i = maxPieSlices - 1; i < byteCounts.size();) {
                leftoverByteCount += byteCounts.get(i).value;
                byteCounts.remove(i);
            }
            byteCounts.add(new GEntry("others", leftoverByteCount));
        }
        if (byPercentage) {
            double tot = 0d;
            for (Iterator<GEntry> iter = byteCounts.iterator(); iter.hasNext();) {
                GEntry ge = iter.next();
                tot += ge.value;
            }
            if (tot > 0d) {
                for (Iterator<GEntry> iter = byteCounts.iterator(); iter.hasNext();) {
                    GEntry ge = iter.next();
                    ge.value = ge.value / tot * 100d;
                }
            }
        }
        for (Iterator<GEntry> iter = byteCounts.iterator(); iter.hasNext();) {
            GEntry ge = iter.next();
            dataset.setValue(ge.name, ge.value);
        }

        PiePlot3D plot = new PiePlot3D(dataset);
        plot.setMaximumLabelWidth(.3);
        plot.setLabelFont(LABEL_FONT);
        plot.setMinimumArcAngleToDraw(0.04d); // About 2 degrees
        plot.setStartAngle(90);
        plot.setDirection(Rotation.CLOCKWISE);
        plot.setForegroundAlpha(0.5f);
        plot.setNoDataMessage("No data to display");
        plot.setInsets(new RectangleInsets(0, 5, 5, 5));
        plot.setToolTipGenerator(new StandardPieToolTipGenerator());
        plot.setLabelGenerator(new StandardPieSectionLabelGenerator("{0} ({2})"));
        JFreeChart chart =
            new JFreeChart(chartTitle, TITLE_FONT, plot, true);
        return chart;
    }

    class GEntry implements Comparable {
        String name;
        double value;
        GEntry(String name, long byteCount) {
            this.name = name;
            this.value = (double)byteCount;
        }
        GEntry(String name, double byteCount) {
            this.name = name;
            this.value = byteCount;
        }

        // NOTE: REVERSED!
        public int compareTo(Object o) {
            if (o instanceof GEntry) {
                GEntry go = (GEntry)o;
                if (value < go.value) return 1;
                else if (value > go.value) return -1;
                else return 0;
            } else {
                throw new ClassCastException("can't compare a GEntry to " + o);
            }
        }
    }
}
