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

import java.awt.*;
import java.sql.*;
import java.util.*;

import com.metavize.mvvm.reporting.*;
import net.sf.jasperreports.engine.JRDefaultScriptlet;
import net.sf.jasperreports.engine.JRScriptletException;
import org.jfree.chart.*;
import org.jfree.chart.plot.*;
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
        double countsA[] = new double[ size ];
        double countsB[] = new double[ size ];
        double countsC[] = new double[ size ];

	// SETUP TIMESTAMPS
	Timestamp startTimestamp, endTimestamp;
	startTimestamp = new Timestamp(startMinuteInMillis);
	endTimestamp = new Timestamp(endMinuteInMillis);

	totalQueryTime = System.currentTimeMillis();

	String sql = "SELECT date_trunc('minute', time_stamp) as time_stamp,"
	    + " sum(nat_sessions), sum(tcp_incoming + tcp_outgoing + udp_incoming + udp_outgoing + icmp_incoming + icmp_outgoing), sum(dmz_sessions)" 
	    + " FROM tr_nat_statistic_evt where"
	    + " time_stamp <= ? AND time_stamp >= ?"
	    + " GROUP BY time_stamp"
	    + " ORDER BY time_stamp";

	int sidx = 1;
	PreparedStatement stmt = con.prepareStatement(sql);
	stmt.setTimestamp(sidx++, endTimestamp);
	stmt.setTimestamp(sidx++, startTimestamp);
	ResultSet rs = stmt.executeQuery();
	totalQueryTime = System.currentTimeMillis() - totalQueryTime;
	totalProcessTime = System.currentTimeMillis();

	// PROCESS EACH ROW
	while (rs.next()) {
	    // GET RESULTS
	    Timestamp eventDate = rs.getTimestamp(1);
	    long countA = rs.getLong(2);
	    long countB = rs.getLong(3);
	    long countC = rs.getLong(4);

	    
	    // ALLOCATE COUNT TO EACH MINUTE WE WERE ALIVE EQUALLY
	    long eventStart = (eventDate.getTime() / MINUTE_INTERVAL) * MINUTE_INTERVAL;
	    long realStart = eventStart < startMinuteInMillis ? (long) 0 : eventStart - startMinuteInMillis;
	    int startInterval = (int)(realStart / MINUTE_INTERVAL)/MINUTES_PER_BUCKET;

	    // COMPUTE COUNTS IN INTERVALS
	    countsA[startInterval%BUCKETS] += countA;
	    countsB[startInterval%BUCKETS] += countB;
	    countsC[startInterval%BUCKETS] += countC;

	}
	try { stmt.close(); } catch (SQLException x) { }


        // POST PROCESS: PRODUCE UNITS OF KBytes/sec., AVERAGED PER DAY, FROM BYTES PER BUCKET
	double averageACount, averageBCount, averageCCount;
        for(int i = 0; i < size; i++) {

	    // MOVING AVERAGE
	    averageACount = averageBCount = averageCCount = 0;
	    int newIndex = 0;
	    int denom = 0;
	    for(int j=0; j<MOVING_AVERAGE_MINUTES; j++){
		newIndex = i-j;
		if( newIndex >= 0 )
		    denom++;
		else
		    continue;
		averageACount += countsA[newIndex] / (double)queries / (double)(60*MINUTES_PER_BUCKET);
		averageBCount += countsB[newIndex] / (double)queries / (double)(60*MINUTES_PER_BUCKET);
		averageCCount += countsC[newIndex] / (double)queries / (double)(60*MINUTES_PER_BUCKET);
	    }
	    averageACount /= denom;
	    averageBCount /= denom;
	    averageCCount /= denom;

	    java.util.Date date = new java.util.Date(startMinuteInMillis + i * MINUTE_INTERVAL * MINUTES_PER_BUCKET );
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
	tsc.addSeries(datasetC);
	tsc.addSeries(datasetB);
	tsc.addSeries(datasetA);

	JFreeChart timeSeriesChart = ChartFactory.createTimeSeriesChart(chartTitle,
									timeAxisLabel,
									valueAxisLabel,
									tsc,
									true,
									true,
									false);

	XYPlot plot = timeSeriesChart.getXYPlot();
	plot.getRenderer().setSeriesPaint(0, new Color(255, 255, 0));
	plot.getRenderer().setSeriesPaint(1, new Color(0, 0, 255));
	plot.getRenderer().setSeriesPaint(2, new Color(0, 255, 0));
	return timeSeriesChart;

    }
}
