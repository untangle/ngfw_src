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

package com.metavize.tran.reporting.reports;

import java.sql.*;
import java.util.*;

import com.metavize.mvvm.reporting.*;
import net.sf.jasperreports.engine.JRDefaultScriptlet;
import net.sf.jasperreports.engine.JRScriptletException;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
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
	this("Traffic", true, true, "Outgoing", "Incoming", "Total");
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

	final int MOVING_AVERAGE_MINUTES = 5;
	final int MINUTES_PER_BUCKET = 1;
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
	Timestamp startTimestamp, endTimestamp;
	startTimestamp = new Timestamp(startMinuteInMillis);
	endTimestamp = new Timestamp(endMinuteInMillis);

	totalQueryTime = System.currentTimeMillis();

	String sql = "SELECT client_intf, date_trunc('minute', create_date) as cd, date_trunc('minute', raze_date) as rd,"
	    + " sum(c2p_bytes), sum(p2s_bytes), sum(s2p_bytes), sum(p2c_bytes)" 
	    + " FROM pl_endp JOIN pl_stats USING (session_id) where ";
	if (!doIncomingSessions || !doOutgoingSessions)
	    sql += "client_intf = ? AND ";
	sql += "create_date <= ? AND raze_date >= ?"
	    + " GROUP BY cd, rd, client_intf"
	    + " ORDER BY cd, rd";
	
	int sidx = 1;
	PreparedStatement stmt = con.prepareStatement(sql);
	if (doIncomingSessions && !doOutgoingSessions) 
	    stmt.setShort(sidx++, (short)0);
	else if (!doIncomingSessions && doOutgoingSessions)
	    stmt.setShort(sidx++, (short)1);        
	stmt.setTimestamp(sidx++, endTimestamp);
	stmt.setTimestamp(sidx++, startTimestamp);
	ResultSet rs = stmt.executeQuery();
	totalQueryTime = System.currentTimeMillis() - totalQueryTime;
	totalProcessTime = System.currentTimeMillis();

	// PROCESS EACH ROW
	while (rs.next()) {
	    // GET RESULTS
	    short clientIntf = rs.getShort(1);
	    Timestamp createDate = rs.getTimestamp(2);
	    Timestamp razeDate = rs.getTimestamp(3);
	    long c2pBytes = rs.getLong(4);
	    long p2sBytes = rs.getLong(5);
	    long s2pBytes = rs.getLong(6);
	    long p2cBytes = rs.getLong(7);
	    // ALLOCATE COUNT TO EACH MINUTE WE WERE ALIVE EQUALLY
	    long sesStart = (createDate.getTime() / MINUTE_INTERVAL) * MINUTE_INTERVAL;
	    int numIntervals = (int)((razeDate.getTime() - createDate.getTime()) / MINUTE_INTERVAL)/MINUTES_PER_BUCKET + 1;
	    long realStart = sesStart < startMinuteInMillis ? (long) 0 : sesStart - startMinuteInMillis;
	    int startInterval = (int)(realStart / MINUTE_INTERVAL)/MINUTES_PER_BUCKET;
	    int endInterval = Math.min(startInterval + numIntervals, periodBuckets);
	    // COMPUTE BYTE COUNTS
	    long incomingByteCount = 0;
	    if (clientIntf == 0)
		incomingByteCount += c2pBytes;
	    else
		incomingByteCount += s2pBytes;
	    double incomingBytesPerInterval = (double)incomingByteCount / numIntervals;
	    long outgoingByteCount = 0;
	    if (clientIntf == 0)
		outgoingByteCount += p2cBytes;
	    else
		outgoingByteCount += p2sBytes;
	    double outgoingBytesPerInterval = (double)outgoingByteCount / numIntervals;
	    // INCREMENT COUNTS
	    if (doThreeSeries) {
		for (int interval = startInterval; interval < endInterval; interval++) {
		    incomingCounts[interval%BUCKETS] += incomingBytesPerInterval;
		    outgoingCounts[interval%BUCKETS] += outgoingBytesPerInterval;
		    counts[interval%BUCKETS] += incomingBytesPerInterval + outgoingBytesPerInterval;
		}
	    }
	    else {
		for (int interval = startInterval; interval < endInterval; interval++) {
		    if (countIncomingBytes)
			counts[interval%BUCKETS] += incomingBytesPerInterval;
		    if (countOutgoingBytes)
			counts[interval%BUCKETS] += outgoingBytesPerInterval;
		}
	    }
	}
	try { stmt.close(); } catch (SQLException x) { }


        // POST PROCESS: PRODUCE UNITS OF KBytes/sec., AVERAGED PER DAY, FROM BYTES PER BUCKET
	double averageTotalCount, averageIncomingCount, averageOutgoingCount;
        for(int i = 0; i < size; i++) {

	    // MOVING AVERAGE
	    averageTotalCount = averageIncomingCount = averageOutgoingCount = 0;
	    int newIndex = 0;
	    int denom = 0;
	    for(int j=0; j<MOVING_AVERAGE_MINUTES; j++){
		newIndex = i-j;
		if( newIndex >= 0 )
		    denom++;
		else
		    continue;
		averageTotalCount += counts[newIndex] / 1024.0d / (double)queries / (double)(60*MINUTES_PER_BUCKET);
		averageIncomingCount += incomingCounts[newIndex] / 1024.0d / (double)queries / (double)(60*MINUTES_PER_BUCKET);
		averageOutgoingCount += outgoingCounts[newIndex] / 1024.0d / (double)queries / (double)(60*MINUTES_PER_BUCKET);
	    }
	    averageTotalCount /= denom;
	    averageIncomingCount /= denom;
	    averageOutgoingCount /= denom;

	    java.util.Date date = new java.util.Date(startMinuteInMillis + i * MINUTE_INTERVAL * MINUTES_PER_BUCKET );
	    dataset.add(new Minute(date), averageTotalCount);
	    if (doThreeSeries) {
		incomingDataset.add(new Minute(date), averageIncomingCount);
		outgoingDataset.add(new Minute(date), averageOutgoingCount);
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
