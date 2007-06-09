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

package com.untangle.tran.nat.reports;

import com.untangle.mvvm.reporting.*;

import java.awt.BasicStroke;
import java.awt.Color;
import java.sql.*;
import java.util.*;
import net.sf.jasperreports.engine.JRDefaultScriptlet;
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

    private String seriesATitle;
    private String seriesBTitle;
    private String seriesCTitle;

    private long totalQueryTime = 0l;
    private long totalProcessTime = 0l;

    public SummaryGraph(){          // out, in, total
        this("Traffic", true, true, "NAT Sessions", "Redirect Sessions", "DMZ Sessions", "Sessions/sec.");
    }

    // Produces a single line graph of one series
    public SummaryGraph(String chartTitle, boolean doOutgoingSessions, boolean doIncomingSessions,
                        boolean countOutgoingBytes, boolean countIncomingBytes,
                        String seriesTitle)
    {
        this.chartTitle = chartTitle;
    }

    // Produces a three series line graph of incoming, outgoing, total.
    public SummaryGraph(String charTitle,
                        boolean doOutgoingSessions, boolean doIncomingSessions,
                        String seriesATitle, String seriesBTitle,
                        String seriesCTitle, String rangeTitle)
    {
        this.chartTitle = chartTitle;
        this.seriesATitle = seriesATitle;
        this.seriesBTitle = seriesBTitle;
        this.seriesCTitle = seriesCTitle;
        this.valueAxisLabel = rangeTitle;
    }

    protected JFreeChart doChart(Connection con) throws JRScriptletException, SQLException
    {
        final int seriesA = 0; // datasetA (primary, first)
        final int seriesB = 1; // datasetB
        final int seriesC = 2; // datasetC

        TimeSeries datasetA = new TimeSeries(seriesATitle, Minute.class);
        TimeSeries datasetB = new TimeSeries(seriesBTitle, Minute.class);
        TimeSeries datasetC = new TimeSeries(seriesCTitle, Minute.class);

        final int BUCKETS = 1440 / MINUTES_PER_BUCKET;

        // TRUNCATE TIME TO MINUTES, COMPUTE NUMBER OF QUERIES
        long startMinuteInMillis = (startDate.getTime() / MINUTE_INTERVAL) * MINUTE_INTERVAL;
        long endMinuteInMillis = (endDate.getTime() / MINUTE_INTERVAL) * MINUTE_INTERVAL;
        long periodMillis = endMinuteInMillis - startMinuteInMillis;
        int periodMinutes = (int)(periodMillis / MINUTE_INTERVAL);
        int queries = periodMinutes/(BUCKETS*MINUTES_PER_BUCKET) + (periodMinutes%(BUCKETS*MINUTES_PER_BUCKET)>0?1:0);

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

        double countsA[] = new double[ size ];
        double countsB[] = new double[ size ];
        double countsC[] = new double[ size ];

        // SETUP TIMESTAMPS
        Timestamp startTimestamp = new Timestamp(startMinuteInMillis);
        Timestamp endTimestamp = new Timestamp(endMinuteInMillis);

        totalQueryTime = System.currentTimeMillis();

        String sql = "SELECT DATE_TRUNC('minute', time_stamp) AS trunc_ts,"
            + " SUM(nat_sessions), SUM(tcp_incoming + tcp_outgoing + udp_incoming + udp_outgoing + icmp_incoming + icmp_outgoing), SUM(dmz_sessions)" 
            + " FROM tr_nat_statistic_evt"
            + " WHERE time_stamp >= ? AND time_stamp < ?"
            + " GROUP BY trunc_ts"
            + " ORDER BY trunc_ts";

        int sidx = 1;
        PreparedStatement stmt = con.prepareStatement(sql);
        stmt.setTimestamp(sidx++, startTimestamp);
        stmt.setTimestamp(sidx++, endTimestamp);
        ResultSet rs = stmt.executeQuery();
        totalQueryTime = System.currentTimeMillis() - totalQueryTime;
        totalProcessTime = System.currentTimeMillis();

        Timestamp eventDate;
        long countA;
        long countB;
        long countC;
        long eventStart;
        long realStart;
        int startInterval;
        int bucket;

        // PROCESS EACH ROW
        while (rs.next()) {
            // GET RESULTS
            eventDate = rs.getTimestamp(1);
            countA = rs.getLong(2);
            countB = rs.getLong(3);
            countC = rs.getLong(4);

            // ALLOCATE COUNT TO EACH MINUTE WE WERE ALIVE EQUALLY
            eventStart = (eventDate.getTime() / MINUTE_INTERVAL) * MINUTE_INTERVAL;
            realStart = eventStart < startMinuteInMillis ? (long) 0 : eventStart - startMinuteInMillis;
            startInterval = (int)(realStart / MINUTE_INTERVAL)/MINUTES_PER_BUCKET;
            bucket = startInterval%BUCKETS;

            // COMPUTE COUNTS IN INTERVALS
            countsA[bucket] += countA;
            countsB[bucket] += countB;
            countsC[bucket] += countC;
        }
        try { stmt.close(); } catch (SQLException x) { }

        // POST PROCESS: PRODUCE UNITS OF KBytes/sec., AVERAGED PER DAY, FROM BYTES PER BUCKET
        double averageACount;
        double averageBCount;
        double averageCCount;
        int newIndex;
        int denom;

        // MOVING AVERAGE
        for(int i = 0; i < size; i++) {
            averageACount = 0;
            averageBCount = 0;
            averageCCount = 0;
            newIndex = 0;
            denom = 0;

            for(int j=0; j<MOVING_AVERAGE_MINUTES; j++){
                newIndex = i-j;
                if( newIndex >= 0 )
                    denom++;
                else
                    continue;

                averageACount += countsA[newIndex] / (double)(queries*(60*MINUTES_PER_BUCKET));
                averageBCount += countsB[newIndex] / (double)(queries*(60*MINUTES_PER_BUCKET));
                averageCCount += countsC[newIndex] / (double)(queries*(60*MINUTES_PER_BUCKET));
            }

            averageACount /= denom;
            averageBCount /= denom;
            averageCCount /= denom;

            java.util.Date date = new java.util.Date(startMinuteInMillis + (i * MINUTE_INTERVAL * MINUTES_PER_BUCKET));
            datasetA.addOrUpdate(new Minute(date), averageACount);
            datasetB.addOrUpdate(new Minute(date), averageBCount);
            datasetC.addOrUpdate(new Minute(date), averageCCount);
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

        TimeSeriesCollection tsc = new TimeSeriesCollection();
        tsc.addSeries(datasetA);
        tsc.addSeries(datasetB);
        tsc.addSeries(datasetC);

        JFreeChart jfChart =
            ChartFactory.createTimeSeriesChart(chartTitle,
                                               timeAxisLabel, valueAxisLabel,
                                               tsc,
                                               true, true, false);
        XYPlot xyPlot = (XYPlot) jfChart.getPlot();
        XYItemRenderer xyIRenderer = xyPlot.getRenderer();
        xyIRenderer.setSeriesStroke(seriesA, new BasicStroke(1.3f));
        xyIRenderer.setSeriesPaint(seriesA, Color.green);
        xyIRenderer.setSeriesStroke(seriesB, new BasicStroke(1.3f));
        xyIRenderer.setSeriesPaint(seriesB, Color.blue);
        xyIRenderer.setSeriesStroke(seriesC, new BasicStroke(1.3f));
        xyIRenderer.setSeriesPaint(seriesC, Color.red);
        return jfChart;
    }
}
