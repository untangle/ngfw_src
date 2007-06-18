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

package com.untangle.node.reporting.reports;

import java.awt.BasicStroke;
import java.awt.Color;
import java.sql.*;
import java.util.*;

import com.untangle.uvm.reporting.*;
import net.sf.jasperreports.engine.JRScriptletException;
import org.jfree.chart.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

public class SummaryGraph extends DayByMinuteTimeSeriesGraph
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

    private long totalQueryTime = 0l;
    private long totalProcessTime = 0l;

    public SummaryGraph(){
        this("Traffic", true, true, "Outgoing Traffic", "Incoming Traffic", "Total", "Kilobytes/sec.");
    }

    // Produces a single line graph of one series
    public SummaryGraph(String chartTitle, boolean doOutgoingSessions, boolean doIncomingSessions,
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
    public SummaryGraph(String charTitle,
                        boolean doOutgoingSessions, boolean doIncomingSessions,
                        String outgoingSeriesTitle, String incomingSeriesTitle,
                        String overallSeriesTitle, String rangeTitle)
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
        this.valueAxisLabel = rangeTitle;
    }

    protected JFreeChart doChart(Connection con) throws JRScriptletException, SQLException
    {
        //final int seriesA = 2; // dataset
        final int seriesOut = 0; // outgoing (primary, first)
        final int seriesIn = 1; // incoming

        TimeSeries dataset = new TimeSeries(seriesTitle, Minute.class);
        TimeSeries outgoingDataset = null, incomingDataset = null;
        if (doThreeSeries) {
            outgoingDataset = new TimeSeries(outgoingSeriesTitle, Minute.class);
            incomingDataset = new TimeSeries(incomingSeriesTitle, Minute.class);
        }

        final int BUCKETS = 1440 / MINUTES_PER_BUCKET;

        // TRUNCATE TIME TO MINUTES, COMPUTE NUMBER OF QUERIES
        long startMinuteInMillis = (startDate.getTime() / MINUTE_INTERVAL) * MINUTE_INTERVAL;
        long endMinuteInMillis = (endDate.getTime() / MINUTE_INTERVAL) * MINUTE_INTERVAL;
        long periodMillis = endMinuteInMillis - startMinuteInMillis;
        int periodMinutes = (int)(periodMillis / MINUTE_INTERVAL);
        int queries = periodMinutes/(BUCKETS*MINUTES_PER_BUCKET) + (periodMinutes%(BUCKETS*MINUTES_PER_BUCKET)>0?1:0);
        int periodBuckets = periodMinutes/(MINUTES_PER_BUCKET) + (periodMinutes%(MINUTES_PER_BUCKET)>0?1:0);

        System.out.println("====== START ======");
        System.out.println("start: " + (new Timestamp(startMinuteInMillis)).toString() );
        System.out.println("end:   " + (new Timestamp(endMinuteInMillis)).toString() );
        System.out.println("mins: " + periodMinutes);
        System.out.println("days: " + queries);
        System.out.println("===================");

        // ALLOCATE COUNTS
        int size;
        if( periodMinutes >= BUCKETS*MINUTES_PER_BUCKET )
            size = BUCKETS;
        else
            size = periodMinutes/(BUCKETS*MINUTES_PER_BUCKET) + (periodMinutes%(BUCKETS*MINUTES_PER_BUCKET)>0?1:0);

        double counts[] = new double[ size ];
        double incomingCounts[] = null;
        double outgoingCounts[] = null;
        if (doThreeSeries) {
            incomingCounts = new double[ size ];
            outgoingCounts = new double[ size ];
        }

        // SETUP TIMESTAMPS
        Timestamp startTimestamp = new Timestamp(startMinuteInMillis);
        Timestamp endTimestamp = new Timestamp(endMinuteInMillis);

        totalQueryTime = System.currentTimeMillis();

        String sql = "SELECT client_intf, DATE_TRUNC('minute', endp.time_stamp) AS cd, DATE_TRUNC('minute', stats.time_stamp) AS rd,"
            + " SUM(c2p_bytes), SUM(p2s_bytes), SUM(s2p_bytes), SUM(p2c_bytes)"
            + " FROM pl_endp endp JOIN pl_stats stats ON (endp.event_id = stats.pl_endp_id)"
            + " WHERE";
        if (!doIncomingSessions || !doOutgoingSessions)
            sql += " client_intf = ? AND";
        sql += " endp.time_stamp >= ? AND stats.time_stamp < ?"
            + " GROUP BY cd, rd, client_intf"
            + " ORDER BY cd, rd";

        int sidx = 1;
        PreparedStatement stmt = con.prepareStatement(sql);
        if (doIncomingSessions && !doOutgoingSessions)
            stmt.setShort(sidx++, (short)0);
        else if (!doIncomingSessions && doOutgoingSessions)
            stmt.setShort(sidx++, (short)1);
        stmt.setTimestamp(sidx++, startTimestamp);
        stmt.setTimestamp(sidx++, endTimestamp);
        ResultSet rs = stmt.executeQuery();
        totalQueryTime = System.currentTimeMillis() - totalQueryTime;
        totalProcessTime = System.currentTimeMillis();

        Timestamp createDate;
        Timestamp razeDate;
        double incomingBytesPerInterval;
        double outgoingBytesPerInterval;
        long c2pBytes;
        long p2sBytes;
        long s2pBytes;
        long p2cBytes;
        long sesStart;
        long realStart;
        long incomingByteCount;
        long outgoingByteCount;
        short clientIntf;
        int numIntervals;
        int startInterval;
        int endInterval;
        int bucket;

        // PROCESS EACH ROW
        while (rs.next()) {
            // GET RESULTS
            clientIntf = rs.getShort(1);
            createDate = rs.getTimestamp(2);
            razeDate = rs.getTimestamp(3);
            c2pBytes = rs.getLong(4);
            p2sBytes = rs.getLong(5);
            s2pBytes = rs.getLong(6);
            p2cBytes = rs.getLong(7);
            // ALLOCATE COUNT TO EACH MINUTE WE WERE ALIVE EQUALLY
            sesStart = (createDate.getTime() / MINUTE_INTERVAL) * MINUTE_INTERVAL;
            numIntervals = (int)((razeDate.getTime() - createDate.getTime()) / MINUTE_INTERVAL)/MINUTES_PER_BUCKET + 1;
            realStart = sesStart < startMinuteInMillis ? (long) 0 : sesStart - startMinuteInMillis;
            startInterval = (int)(realStart / MINUTE_INTERVAL)/MINUTES_PER_BUCKET;
            endInterval = Math.min(startInterval + numIntervals, periodBuckets);
            // COMPUTE BYTE COUNTS
            incomingByteCount = 0;
            if (clientIntf == 0)
                incomingByteCount += c2pBytes;
            else
                incomingByteCount += s2pBytes;
            incomingBytesPerInterval = (double)incomingByteCount / numIntervals;
            outgoingByteCount = 0;
            if (clientIntf == 0)
                outgoingByteCount += p2cBytes;
            else
                outgoingByteCount += p2sBytes;
            outgoingBytesPerInterval = (double)outgoingByteCount / numIntervals;

            // INCREMENT COUNTS
            if (doThreeSeries) {
                for (int interval = startInterval; interval < endInterval; interval++) {
                    bucket = interval%BUCKETS;
                    incomingCounts[bucket] += incomingBytesPerInterval;
                    outgoingCounts[bucket] += outgoingBytesPerInterval;
                    counts[bucket] += incomingBytesPerInterval + outgoingBytesPerInterval;
                }
            }
            else {
                for (int interval = startInterval; interval < endInterval; interval++) {
                    bucket = interval%BUCKETS;
                    if (countIncomingBytes)
                        counts[bucket] += incomingBytesPerInterval;
                    if (countOutgoingBytes)
                        counts[bucket] += outgoingBytesPerInterval;
                }
            }
        }
        try { stmt.close(); } catch (SQLException x) { }

        // POST PROCESS: PRODUCE UNITS OF KBytes/sec., AVERAGED PER DAY, FROM BYTES PER BUCKET
        double averageTotalCount;
        double averageIncomingCount;
        double averageOutgoingCount;
        int newIndex;
        int denom;

        // MOVING AVERAGE
        for(int i = 0; i < size; i++) {
            averageTotalCount = 0;
            averageIncomingCount = 0;
            averageOutgoingCount = 0;
            newIndex = 0;
            denom = 0;

            for(int j=0; j<MOVING_AVERAGE_MINUTES; j++){
                newIndex = i-j;
                if( newIndex >= 0 )
                    denom++;
                else
                    continue;

                averageTotalCount += counts[newIndex] / 1024.0d / (double)(queries*(60*MINUTES_PER_BUCKET));
                averageIncomingCount += incomingCounts[newIndex] / 1024.0d / (double)(queries*(60*MINUTES_PER_BUCKET));
                averageOutgoingCount += outgoingCounts[newIndex] / 1024.0d / (double)(queries*(60*MINUTES_PER_BUCKET));
            }

            averageTotalCount /= denom;
            averageIncomingCount /= denom;
            averageOutgoingCount /= denom;

            java.util.Date date = new java.util.Date(startMinuteInMillis + (i * MINUTE_INTERVAL * MINUTES_PER_BUCKET));
            dataset.addOrUpdate(new Minute(date), averageTotalCount);
            if (doThreeSeries) {
                incomingDataset.addOrUpdate(new Minute(date), averageIncomingCount);
                outgoingDataset.addOrUpdate(new Minute(date), averageOutgoingCount);
            }
        }
        totalProcessTime = System.currentTimeMillis() - totalProcessTime;
        System.out.println("====== RESULTS ======");
        System.out.println("TOTAL query time:   "
                           + totalQueryTime/1000 + "s"
                           + " (" + ((float)totalQueryTime/(float)(totalQueryTime+totalProcessTime))  + ")");
        System.out.println("TOTAL process time: "
                           + totalProcessTime/1000 + "s"
                           + " (" + ((float)totalProcessTime/(float)(totalQueryTime+totalProcessTime))  + ")");
        System.out.println("=====================");

        //TimeSeriesCollection tsc = new TimeSeriesCollection(dataset);
        TimeSeriesCollection tsc = new TimeSeriesCollection();
        if (doThreeSeries) {
            tsc.addSeries(outgoingDataset);
            tsc.addSeries(incomingDataset);
        }

        JFreeChart jfChart =
            ChartFactory.createTimeSeriesChart(chartTitle,
                                               timeAxisLabel, valueAxisLabel,
                                               tsc,
                                               true, true, false);
        XYPlot xyPlot = (XYPlot) jfChart.getPlot();
        XYItemRenderer xyIRenderer = xyPlot.getRenderer();
        //xyIRenderer.setSeriesStroke(seriesA, new BasicStroke(1.3f));
        //xyIRenderer.setSeriesPaint(seriesA, Color.red);
        xyIRenderer.setSeriesStroke(seriesOut, new BasicStroke(1.3f));
        xyIRenderer.setSeriesPaint(seriesOut, Color.green);
        xyIRenderer.setSeriesStroke(seriesIn, new BasicStroke(1.3f));
        xyIRenderer.setSeriesPaint(seriesIn, Color.blue);
        return jfChart;
    }
}
