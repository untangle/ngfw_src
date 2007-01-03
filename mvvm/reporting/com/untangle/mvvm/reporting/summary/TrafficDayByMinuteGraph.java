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

package com.untangle.mvvm.reporting.summary;

import java.sql.*;
import java.util.*;

import com.untangle.mvvm.reporting.*;
import net.sf.jasperreports.engine.JRScriptletException;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;


public class TrafficDayByMinuteGraph extends DayByMinuteTimeSeriesGraph
{
    private String chartTitle;

    private boolean doOutgoingSessions;
    private boolean doIncomingSessions;
    private boolean countOutgoingBytes;
    private boolean countIncomingBytes;

    private String seriesTitle;

    private boolean doThreeSeries;
    private String incomingSeriesTitle;
    private String outgoingSeriesTitle;

    // Produces a single line graph of one series
    public TrafficDayByMinuteGraph(String chartTitle, boolean doOutgoingSessions, boolean doIncomingSessions,
                                   boolean countOutgoingBytes, boolean countIncomingBytes,
                                   String seriesTitle)
    {
        this.chartTitle = chartTitle;
        this.doOutgoingSessions = doOutgoingSessions;
        this.doIncomingSessions = doIncomingSessions;
        this.countOutgoingBytes = countOutgoingBytes;
        this.countIncomingBytes = countIncomingBytes;
        this.doThreeSeries = false;
        this.seriesTitle = seriesTitle;
    }

    // Produces a three series line graph of incoming, outgoing, total.
    public TrafficDayByMinuteGraph(String charTitle,
                                   boolean doOutgoingSessions, boolean doIncomingSessions,
                                   String outgoingSeriesTitle, String incomingSeriesTitle,
                                   String overallSeriesTitle)
    {
        this.chartTitle = chartTitle;
        this.doOutgoingSessions = doOutgoingSessions;
        this.doIncomingSessions = doIncomingSessions;
        this.countOutgoingBytes = countOutgoingBytes;
        this.countIncomingBytes = countIncomingBytes;
        this.doThreeSeries = true;
        this.doOutgoingSessions = true;
        this.doIncomingSessions = true;
        this.seriesTitle = overallSeriesTitle;
        this.outgoingSeriesTitle = outgoingSeriesTitle;
        this.incomingSeriesTitle = incomingSeriesTitle;
    }

    protected JFreeChart doChart(Connection con) throws JRScriptletException, SQLException
    {
        TimeSeries dataset = new TimeSeries(seriesTitle, Minute.class);
        TimeSeries outgoingDataset = null, incomingDataset = null;
        if (doThreeSeries) {
            outgoingDataset = new TimeSeries(outgoingSeriesTitle, Minute.class);
            incomingDataset = new TimeSeries(incomingSeriesTitle, Minute.class);
        }

        // Load up the datasets
        String sql = "SELECT client_intf, create_date, raze_date, c2p_bytes, p2s_bytes, s2p_bytes, p2c_bytes FROM pl_endp endp JOIN pl_stats stats ON endp.event_id = stats.pl_endp_id where ";
        if (!doIncomingSessions || !doOutgoingSessions)
            sql += "client_intf = ? AND ";
        sql += "create_date <= ? AND raze_date >= ? ORDER BY create_date";
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

        // Truncate the start time to the minute.
        long ourStart = startDate.getTime();
        ourStart = (ourStart / MINUTE_INTERVAL) * MINUTE_INTERVAL;
        long ourEnd = endDate.getTime();
        ourEnd = (ourEnd / MINUTE_INTERVAL) * MINUTE_INTERVAL;
        long ourInterval = ourEnd - ourStart;
        int ourMins = (int)(ourInterval / MINUTE_INTERVAL);
        // Protect against really big dataset.  Should usally just be 1440.
        if (ourMins > 1441) {
            ourMins = 1440;
        }
        double counts[] = new double[ourMins];
        double incomingCounts[] = null;
        double outgoingCounts[] = null;
        if (doThreeSeries) {
            incomingCounts = new double[ourMins];
            outgoingCounts = new double[ourMins];
        }

        // Process each row.
        while (rs.next()) {
            short clientIntf = rs.getShort(1);
            Timestamp createDate = rs.getTimestamp(2);
            Timestamp razeDate = rs.getTimestamp(3);
            long c2pBytes = rs.getLong(4);
            long p2sBytes = rs.getLong(5);
            long s2pBytes = rs.getLong(6);
            long p2cBytes = rs.getLong(7);
            // Allocate count to each minute we were alive, equally.
            long sesStart = (createDate.getTime() / MINUTE_INTERVAL) * MINUTE_INTERVAL;
            int numIntervals = (int)((razeDate.getTime() - createDate.getTime()) / MINUTE_INTERVAL) + 1;
            long realStart = sesStart < ourStart ? (long) 0 : sesStart - ourStart;
            int startInterval = (int)(realStart / MINUTE_INTERVAL);
            int endInterval = Math.min(startInterval + numIntervals, ourMins);

            long incomingByteCount = 0;
            if (clientIntf == 0) {
                incomingByteCount += c2pBytes;
            } else {
                incomingByteCount += s2pBytes;
            }
            double incomingBytesPerInterval = (double)incomingByteCount / numIntervals;
            long outgoingByteCount = 0;
            if (clientIntf == 0) {
                outgoingByteCount += p2cBytes;
            } else {
                outgoingByteCount += p2sBytes;
            }
            double outgoingBytesPerInterval = (double)outgoingByteCount / numIntervals;

            if (doThreeSeries) {
                for (int interval = startInterval; interval < endInterval; interval++) {
                    incomingCounts[interval] += incomingBytesPerInterval;
                    outgoingCounts[interval] += outgoingBytesPerInterval;
                    counts[interval] += incomingBytesPerInterval + outgoingBytesPerInterval;
                }
            } else {
                for (int interval = startInterval; interval < endInterval; interval++) {
                    if (countIncomingBytes)
                        counts[interval] += incomingBytesPerInterval;
                    if (countOutgoingBytes)
                        counts[interval] += outgoingBytesPerInterval;
                }
            }
        }

        // Post-process to produce the dataset(s) scaled to KBytes/sec.
        for (int i = 0; i < ourMins; i++) {
            java.util.Date date = new java.util.Date(ourStart + i * MINUTE_INTERVAL);
            double byteCountPerMin = counts[i];
            double kBPerSec = byteCountPerMin * (60.0d / 1024.0d);
            // System.out.println("at " + date + ":\t" + kBPerSec);
            dataset.add(new Minute(date), kBPerSec);
            if (doThreeSeries) {
                incomingDataset.add(new Minute(date), incomingCounts[i] * (60.0d / 1024.0d));
                outgoingDataset.add(new Minute(date), outgoingCounts[i] * (60.0d / 1024.0d));
            }
        }
        try { stmt.close(); } catch (SQLException x) { }

        TimeSeriesCollection tsc = new TimeSeriesCollection(dataset);
        if (doThreeSeries) {
            tsc.addSeries(incomingDataset);
            tsc.addSeries(outgoingDataset);
        }

        return ChartFactory.createTimeSeriesChart(chartTitle,
                                                  timeAxisLabel,
                                                  valueAxisLabel,
                                                  tsc,
                                                  true,
                                                  true,
                                                  false);
    }
}
