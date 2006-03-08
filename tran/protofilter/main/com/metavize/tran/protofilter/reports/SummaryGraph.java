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

package com.metavize.tran.protofilter.reports;

import com.metavize.mvvm.reporting.*;

import java.awt.BasicStroke;
import java.awt.Color;
import java.sql.*;
import java.util.*;
import net.sf.jasperreports.engine.JRDefaultScriptlet;
import net.sf.jasperreports.engine.JRScriptletException;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
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
	this("Traffic", true, true, "Clean/Passed", "Rogue Protocols Detected/Blocked", "Total", "Detected Protocols/min.");
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
        //final int seriesC = 2; // datasetC

        TimeSeries datasetA = new TimeSeries(seriesATitle, Minute.class);
        TimeSeries datasetB = new TimeSeries(seriesBTitle, Minute.class);
        //TimeSeries datasetC = new TimeSeries(seriesCTitle, Minute.class);

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
        //double countsC[] = new double[ size ];

	// SETUP TIMESTAMPS
	Timestamp startTimestamp, endTimestamp;
	startTimestamp = new Timestamp(startMinuteInMillis);
	endTimestamp = new Timestamp(endMinuteInMillis);

	totalQueryTime = System.currentTimeMillis();

	String sql = "SELECT date_trunc('minute', time_stamp) as time_stamp,"
	    + " count(case blocked when true then 1 else null end), count(*)"
	    + " FROM tr_protofilter_evt WHERE"
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
	    long countA = rs.getLong(3) - rs.getLong(2);
	    long countB = rs.getLong(2);
	    //long countC = rs.getLong(3);
	    
	    // ALLOCATE COUNT TO EACH MINUTE WE WERE ALIVE EQUALLY
	    long eventStart = (eventDate.getTime() / MINUTE_INTERVAL) * MINUTE_INTERVAL;
	    long realStart = eventStart < startMinuteInMillis ? (long) 0 : eventStart - startMinuteInMillis;
	    int startInterval = (int)(realStart / MINUTE_INTERVAL)/MINUTES_PER_BUCKET;

	    // COMPUTE COUNTS IN INTERVALS
	    countsA[startInterval%BUCKETS] += countA;
	    countsB[startInterval%BUCKETS] += countB;
	    //countsC[startInterval%BUCKETS] += countC;
	}
	try { stmt.close(); } catch (SQLException x) { }


        // POST PROCESS: PRODUCE UNITS OF KBytes/sec., AVERAGED PER DAY, FROM BYTES PER BUCKET
	double averageACount;
	double averageBCount;
	//double averageCCount;
        for(int i = 0; i < size; i++) {

	    // MOVING AVERAGE
	    averageACount = 0;
	    averageBCount = 0;
	    //averageCCount = 0;
	    int newIndex = 0;
	    int denom = 0;
	    for(int j=0; j<MOVING_AVERAGE_MINUTES; j++){
		newIndex = i-j;
		if( newIndex >= 0 )
		    denom++;
		else
		    continue;
		averageACount += countsA[newIndex] / (double)queries;
		averageBCount += countsB[newIndex] / (double)queries;
		//averageCCount += countsC[newIndex] / (double)queries;
	    }
	    averageACount /= denom;
	    averageBCount /= denom;
	    //averageCCount /= denom;

	    java.util.Date date = new java.util.Date(startMinuteInMillis + i * MINUTE_INTERVAL * MINUTES_PER_BUCKET );
	    datasetA.addOrUpdate(new Minute(date), averageACount);
	    datasetB.addOrUpdate(new Minute(date), averageBCount);
	    //datasetC.addOrUpdate(new Minute(date), averageCCount);
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
	//tsc.addSeries(datasetC);

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
        xyIRenderer.setSeriesPaint(seriesB, Color.red);
        //xyIRenderer.setSeriesStroke(seriesC, new BasicStroke(1.3f));
        //xyIRenderer.setSeriesPaint(seriesC, Color.blue);
        return jfChart;
    }
}
