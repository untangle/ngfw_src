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

package com.untangle.gui.transform;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.*;
import javax.swing.*;

import com.untangle.gui.util.*;
import com.untangle.mvvm.tran.*;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.StandardCategoryToolTipGenerator;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.urls.StandardCategoryURLGenerator;
import org.jfree.chart.urls.StandardXYURLGenerator;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleInsets;
import org.jfree.ui.TextAnchor;

public class MTransformDisplayJPanel extends javax.swing.JPanel implements Shutdownable {

    // GENERAL TRANSFORM
    protected MTransformJPanel mTransformJPanel;

    // GENERAL DISPLAY
    protected boolean getUpdateActivity(){ return true; }
    protected boolean getUpdateSessions(){ return true; }
    protected boolean getUpdateThroughput(){ return true; }
    protected UpdateGraphThread updateGraphThread;

    // THROUGHPUT & SESSION COUNT DISPLAY
    private static long SLEEP_MILLIS = 1000l;
    protected ChartPanel throughputChartJPanel;
    protected TimeSeriesCollection throughputDynamicTimeSeriesCollection;

    protected ChartPanel sessionChartJPanel;
    protected TimeSeriesCollection sessionDynamicTimeSeriesCollection;

    // ACTIVITY COUNT DISPLAY
    protected ChartPanel activityChartJPanel;
    private static String activitySeriesString = "activitySeries";  // row key
    private static String activityString0 = "Activity 0";  // row key
    private static String activityString1 = "Activity 1";  // row key
    private static String activityString2 = "Activity 2";  // row key
    private static String activityString3 = "Activity 3";  // row key
    private DefaultCategoryDataset dataset = new DefaultCategoryDataset();


    // Variables declaration - do not modify//GEN-BEGIN:variables
    protected javax.swing.JLabel activity0JLabel;
    protected javax.swing.JLabel activity1JLabel;
    protected javax.swing.JLabel activity2JLabel;
    protected javax.swing.JLabel activity3JLabel;
    private javax.swing.JPanel activityContentJPanel;
    private javax.swing.JLabel activityJLabel;
    private javax.swing.JPanel activityJPanel;
    private javax.swing.JPanel inputJPanel;
    protected javax.swing.JLabel sessionRequestTotalJLabel;
    protected javax.swing.JLabel sessionTotalJLabel;
    private javax.swing.JLabel sessionsJLabel;
    private javax.swing.JPanel sessionsJPanel;
    protected javax.swing.JLabel throughputJLabel;
    protected javax.swing.JLabel throughputTotalJLabel;
    // End of variables declaration//GEN-END:variables


    public MTransformDisplayJPanel(MTransformJPanel mTransformJPanel) {
	setDoubleBuffered(true);
        this.mTransformJPanel = mTransformJPanel;

        throughputDynamicTimeSeriesCollection = new TimeSeriesCollection();
        TimeSeries tsRate = new TimeSeries("Rate", Second.class);
        tsRate.setMaximumItemCount(60);
        throughputDynamicTimeSeriesCollection.addSeries(tsRate);
        throughputChartJPanel = createLineChart(throughputDynamicTimeSeriesCollection, true);

        sessionDynamicTimeSeriesCollection = new TimeSeriesCollection();
        TimeSeries tsCount = new TimeSeries("Session Count", Second.class);
        tsCount.setMaximumItemCount(60);
        TimeSeries tsReq = new TimeSeries("Session Requests", Second.class);
        tsReq.setMaximumItemCount(60);
        sessionDynamicTimeSeriesCollection.addSeries(tsCount);
        sessionDynamicTimeSeriesCollection.addSeries(tsReq);
        sessionChartJPanel = createLineChart(sessionDynamicTimeSeriesCollection, false);

        activityChartJPanel = createBarChart(dataset);

        initComponents();

        if( !getUpdateActivity() ){
            this.remove(activityJPanel);
            this.remove(activityJLabel);
        }
        if( !getUpdateSessions() ){
            this.remove(sessionChartJPanel);
            this.remove(sessionsJLabel);
        }
        if( !getUpdateThroughput() ){
            this.remove(throughputChartJPanel);
            this.remove(throughputJLabel);
        }
        updateGraphThread = new UpdateGraphThread();
	mTransformJPanel.addShutdownable("UpdateGraphThread", updateGraphThread);
    }

    public void doShutdown(){
	mTransformJPanel.mTransformControlsJPanel().doShutdown();
    }

    public void setDoVizUpdates(boolean doVizUpdates){
        updateGraphThread.setDoVizUpdates( doVizUpdates );
    }

    private ChartPanel createBarChart(CategoryDataset dataset) {

        JFreeChart jFreeChart = MVChartFactory.createBarChart(null,null,null, dataset, PlotOrientation.HORIZONTAL, false,false,false);
        jFreeChart.setBackgroundPaint(Color.BLACK);
        jFreeChart.setBorderVisible(false);

        // PLOT
        CategoryPlot plot = jFreeChart.getCategoryPlot();
        plot.setBackgroundPaint(Color.BLACK);
        plot.setOutlinePaint(null);
        plot.getRenderer().setSeriesPaint(0, new Color(.4f, .4f, 1f, .5f));
        plot.getRenderer().setSeriesOutlinePaint(0, new Color(.4f, .4f, 1f, 1f));
        plot.setInsets(new RectangleInsets(0,0,0,2), false);
        plot.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinesVisible(false);

        // X AXIS
        CategoryAxis catAxis = plot.getDomainAxis();
        catAxis.setVisible(false);
        catAxis.setAxisLineVisible(true);
        catAxis.setAxisLinePaint(Color.BLACK);
        catAxis.setTickMarksVisible(false);

        // Y AXIS
        NumberAxis axis = (NumberAxis) plot.getRangeAxis();
        axis.setVisible(false);
        axis.setAxisLineVisible(false);
        //axis.setTickLabelFont(new java.awt.Font("Dialog", 0, 8));
        //axis.setTickLabelPaint(new Color(130, 130, 130));
        axis.setInverted(false);
        axis.setAutoRangeIncludesZero(true);
        axis.setLowerBound(0.0d);
        axis.setUpperBound(100.0d);

        // CHART
        ChartPanel chartPanel = new ChartPanel(jFreeChart, false, false, false, false, false);
        chartPanel.setOpaque(true);
        chartPanel.setMinimumDrawHeight(20);
        chartPanel.setMinimumDrawWidth(20);
        chartPanel.setMouseZoomable(false);

        return chartPanel;
    }


    private ChartPanel createLineChart(XYDataset dataset, boolean showDecimal) {

        JFreeChart jFreeChart = MVChartFactory.createXYLineChart(null,null,null, dataset, PlotOrientation.VERTICAL, false,false,false);
        jFreeChart.setBackgroundPaint(Color.BLACK);
        XYPlot plot = jFreeChart.getXYPlot();
        plot.setBackgroundPaint(Color.BLACK);
        plot.setOutlinePaint(null);

        plot.getRenderer().setSeriesPaint(0, new Color(.5f, .5f, 1f));
        plot.getRenderer().setSeriesPaint(1, new Color(.5f, .25f, .5f));

        plot.setInsets(new RectangleInsets(4,0,6,2), false);
        plot.setDomainCrosshairVisible(false);
        plot.setDomainGridlinesVisible(false);
        plot.setRangeCrosshairVisible(false);
        plot.setRangeGridlinesVisible(false);
        plot.setFixedDomainAxisSpace(null);

        NumberAxis axis = (NumberAxis) plot.getDomainAxis();
        axis.setVisible(false);
        axis.setFixedAutoRange(60000.0);  // 60 seconds
        axis.setAxisLineVisible(false);
        axis.setTickMarksVisible(false);

        axis = (NumberAxis) plot.getRangeAxis();
        axis.setVisible(true);
        axis.setAutoRange(true);
        axis.setAutoRangeIncludesZero(false);
        axis.setAutoRangeStickyZero(false);
        axis.setAxisLineVisible(true);
        axis.setTickLabelFont(new java.awt.Font("Dialog", 0, 8));
        axis.setTickLabelPaint(new Color(190, 190, 190));

        if( showDecimal == false ){
            DecimalFormat decimalFormat = new DecimalFormat();
            decimalFormat.setMaximumIntegerDigits(7);
            decimalFormat.setMaximumFractionDigits(1);
            decimalFormat.setMinimumIntegerDigits(1);
            decimalFormat.setMinimumFractionDigits(0);
            axis.setNumberFormatOverride(decimalFormat);
            //axis.setTickUnit( new NumberTickUnit(1d) );
        }
        else{
            DecimalFormat decimalFormat = new DecimalFormat();
            decimalFormat.setMaximumIntegerDigits(7);
            decimalFormat.setMaximumFractionDigits(3);
            decimalFormat.setMinimumIntegerDigits(1);
            decimalFormat.setMinimumFractionDigits(0);
            axis.setNumberFormatOverride(decimalFormat);
        }

        ChartPanel chartPanel = new ChartPanel(jFreeChart, false, false, false, false, false);
        chartPanel.setOpaque(true);
        chartPanel.setMinimumDrawHeight(20);
        chartPanel.setMinimumDrawWidth(20);
        chartPanel.setMouseZoomable(false);

        return chartPanel;
    }



    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        inputJPanel = throughputChartJPanel;
        throughputTotalJLabel = new javax.swing.JLabel();
        sessionsJPanel = sessionChartJPanel;
        sessionTotalJLabel = new javax.swing.JLabel();
        sessionRequestTotalJLabel = new javax.swing.JLabel();
        activityJPanel = new javax.swing.JPanel();
        activity0JLabel = new javax.swing.JLabel();
        activity1JLabel = new javax.swing.JLabel();
        activity2JLabel = new javax.swing.JLabel();
        activity3JLabel = new javax.swing.JLabel();
        activityContentJPanel = activityChartJPanel;
        throughputJLabel = new javax.swing.JLabel();
        sessionsJLabel = new javax.swing.JLabel();
        activityJLabel = new javax.swing.JLabel();

        setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        setMaximumSize(new java.awt.Dimension(498, 65));
        setMinimumSize(new java.awt.Dimension(498, 65));
        setOpaque(false);
        setPreferredSize(new java.awt.Dimension(498, 65));
        inputJPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        inputJPanel.setBorder(new javax.swing.border.BevelBorder(javax.swing.border.BevelBorder.LOWERED));
        inputJPanel.setOpaque(false);
        throughputTotalJLabel.setFont(new java.awt.Font("Dialog", 0, 8));
        throughputTotalJLabel.setForeground(new java.awt.Color(190, 190, 190));
        throughputTotalJLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        throughputTotalJLabel.setText(" ");
        throughputTotalJLabel.setDoubleBuffered(true);
        inputJPanel.add(throughputTotalJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(5, 5, 74, -1));

        add(inputJPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(411, 14, 84, 64));

        sessionsJPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        sessionsJPanel.setBorder(new javax.swing.border.BevelBorder(javax.swing.border.BevelBorder.LOWERED));
        sessionsJPanel.setOpaque(false);
        sessionTotalJLabel.setFont(new java.awt.Font("Dialog", 0, 8));
        sessionTotalJLabel.setForeground(new java.awt.Color(190, 190, 190));
        sessionTotalJLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        sessionTotalJLabel.setText(" ");
        sessionTotalJLabel.setDoubleBuffered(true);
        sessionsJPanel.add(sessionTotalJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(5, 5, 74, -1));

        sessionRequestTotalJLabel.setFont(new java.awt.Font("Dialog", 0, 8));
        sessionRequestTotalJLabel.setForeground(new java.awt.Color(190, 190, 190));
        sessionRequestTotalJLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        sessionRequestTotalJLabel.setText(" ");
        sessionRequestTotalJLabel.setDoubleBuffered(true);
        sessionsJPanel.add(sessionRequestTotalJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(5, 49, 74, -1));

        add(sessionsJPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(318, 14, 84, 64));

        activityJPanel.setLayout(new java.awt.GridBagLayout());

        activityJPanel.setBorder(new javax.swing.border.BevelBorder(javax.swing.border.BevelBorder.LOWERED));
        activity0JLabel.setFont(new java.awt.Font("Dialog", 0, 9));
        activity0JLabel.setForeground(new java.awt.Color(190, 190, 190));
        activity0JLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        activity0JLabel.setText(" ");
        activity0JLabel.setDoubleBuffered(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.25;
        gridBagConstraints.insets = new java.awt.Insets(4, 3, 2, 3);
        activityJPanel.add(activity0JLabel, gridBagConstraints);

        activity1JLabel.setFont(new java.awt.Font("Dialog", 0, 9));
        activity1JLabel.setForeground(new java.awt.Color(190, 190, 190));
        activity1JLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        activity1JLabel.setText(" ");
        activity1JLabel.setDoubleBuffered(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.25;
        gridBagConstraints.insets = new java.awt.Insets(3, 3, 3, 3);
        activityJPanel.add(activity1JLabel, gridBagConstraints);

        activity2JLabel.setFont(new java.awt.Font("Dialog", 0, 9));
        activity2JLabel.setForeground(new java.awt.Color(190, 190, 190));
        activity2JLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        activity2JLabel.setText(" ");
        activity2JLabel.setDoubleBuffered(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.25;
        gridBagConstraints.insets = new java.awt.Insets(3, 3, 3, 3);
        activityJPanel.add(activity2JLabel, gridBagConstraints);

        activity3JLabel.setFont(new java.awt.Font("Dialog", 0, 9));
        activity3JLabel.setForeground(new java.awt.Color(190, 190, 190));
        activity3JLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        activity3JLabel.setText(" ");
        activity3JLabel.setDoubleBuffered(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.25;
        gridBagConstraints.insets = new java.awt.Insets(2, 3, 4, 3);
        activityJPanel.add(activity3JLabel, gridBagConstraints);

        activityContentJPanel.setOpaque(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        activityJPanel.add(activityContentJPanel, gridBagConstraints);

        add(activityJPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(245, 14, 64, 64));

        throughputJLabel.setFont(new java.awt.Font("Dialog", 0, 9));
        throughputJLabel.setForeground(new java.awt.Color(150, 150, 150));
        throughputJLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        throughputJLabel.setText("data rate (KBps)");
        throughputJLabel.setDoubleBuffered(true);
        add(throughputJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(411, 0, 82, -1));

        sessionsJLabel.setFont(new java.awt.Font("Dialog", 0, 9));
        sessionsJLabel.setForeground(new java.awt.Color(150, 150, 150));
        sessionsJLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        sessionsJLabel.setText("sessions");
        sessionsJLabel.setDoubleBuffered(true);
        add(sessionsJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(318, 0, 82, -1));

        activityJLabel.setFont(new java.awt.Font("Dialog", 0, 9));
        activityJLabel.setForeground(new java.awt.Color(150, 150, 150));
        activityJLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        activityJLabel.setText("activity");
        activityJLabel.setDoubleBuffered(true);
        add(activityJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(245, 0, 62, -1));

    }//GEN-END:initComponents


    private class UpdateGraphThread extends Thread implements Shutdownable {

        private TransformStats currentStats = null;
        private long sessionCountCurrent = 0;
        private long sessionCountTotal = 0;
        private long sessionRequestTotal = 0;
        private long sessionRequestLast = 0;
        private long byteCountCurrent = 0;
        private long byteCountLast = 0;

        private double sessionFactor;
        private double throughputFactor;
        private DoubleFIFO activity0Count = new DoubleFIFO(60);
        private DoubleFIFO activity1Count = new DoubleFIFO(60);
        private DoubleFIFO activity2Count = new DoubleFIFO(60);
        private DoubleFIFO activity3Count = new DoubleFIFO(60);

        private int newestIndex;
        private double activityTotal;
        private UpdateVizRunnable updateVizRunnable = new UpdateVizRunnable();
        private volatile boolean firstRun = true;

        public UpdateGraphThread(){
            super("MVCLIENT-UpdateGraphThread: " + MTransformDisplayJPanel.this.mTransformJPanel.getTransformDesc().getDisplayName());
            setDaemon(true);
            start();
        }

        // GRAPH CONTROL //////
        private volatile boolean stop;
        public synchronized void doShutdown(){
                stop = true;
                notify();
                interrupt();
        }
        private boolean doVizUpdates;
        public synchronized void setDoVizUpdates(final boolean doVizUpdatesX){
                if( (doVizUpdates==false) && (doVizUpdatesX==true) )
                    firstRun = true;
                doVizUpdates = doVizUpdatesX;
                if( doVizUpdatesX )
                    notify();
        }
        //////////////////////

        public void run() {
            while(!stop){
                try{
                    computeViz();
                    synchronized(this){
                        if(!doVizUpdates || firstRun)
                            updateVizRunnable.setDoReset(true);
                        else
                            updateVizRunnable.setDoReset(false);
                    }
                    SwingUtilities.invokeAndWait( updateVizRunnable );                
                    synchronized(this){
                        firstRun = false;
                        if( !doVizUpdates )
                            wait();                        
                    }
                    Transform fakeTransform = Util.getStatsCache().getFakeTransform(mTransformJPanel.getTid());
                    if( fakeTransform != null ) // only dereference stats if they exist, otherwise sleep/wait
                        currentStats = fakeTransform.getStats();
                    Thread.sleep(SLEEP_MILLIS);
                }
                catch(InterruptedException e){
                    continue;
                }
                catch(java.lang.reflect.InvocationTargetException e){
                    Util.handleExceptionNoRestart("Error updating viz", e);
                }
            }
        }

	private void computeViz(){
	    if(currentStats == null)
            return;
	    // UPDATE COUNTS
	    sessionCountCurrent = currentStats.tcpSessionCount() + currentStats.udpSessionCount();
	    sessionCountTotal = currentStats.tcpSessionTotal() + currentStats.udpSessionTotal();
	    sessionRequestTotal = currentStats.tcpSessionRequestTotal() + currentStats.udpSessionRequestTotal();
	    byteCountCurrent = currentStats.c2tBytes() + currentStats.s2tBytes();
	    // (RESET COUNTS IF NECESSARY)
	    if( (byteCountLast == 0) || (byteCountLast > byteCountCurrent) )
		byteCountLast = byteCountCurrent;
	    if( (sessionRequestLast == 0) || (sessionRequestLast > sessionRequestTotal) )
		sessionRequestLast = sessionRequestTotal;
	    // ADD TO BARS
	    activity0Count.add( (double) currentStats.getCount(Transform.GENERIC_0_COUNTER) );
	    activity1Count.add( (double) currentStats.getCount(Transform.GENERIC_1_COUNTER) );
	    activity2Count.add( (double) currentStats.getCount(Transform.GENERIC_2_COUNTER) );
	    activity3Count.add( (double) currentStats.getCount(Transform.GENERIC_3_COUNTER) );
	}

	
	private class UpdateVizRunnable implements Runnable {
        private boolean doReset;
        public void setDoReset(boolean doReset){ this.doReset = doReset; }
	    public void run(){
            if(doReset){
                // RESET BARS
                activity0Count.reset();
                activity1Count.reset();
                activity2Count.reset();
                activity3Count.reset();
                // RESET GRAPHS
                long now = System.currentTimeMillis();
                if( MTransformDisplayJPanel.this.getUpdateSessions() ){
                    for(int i=60; i>0; i--){
                        Second second = new Second(new Date(now - i * 1000));
                        sessionDynamicTimeSeriesCollection.getSeries(0).addOrUpdate(second, 0f);
                        sessionDynamicTimeSeriesCollection.getSeries(1).addOrUpdate(second, 0f);
                    }
                }
                if( MTransformDisplayJPanel.this.getUpdateThroughput() ){
                    for(int i=60; i>0; i--){
                        Second second = new Second(new Date(now - i * 1000));
                        throughputDynamicTimeSeriesCollection.getSeries(0).addOrUpdate(second, 0f);
                    }
                }
                // RESET COUNTS
                sessionCountCurrent = sessionCountTotal = 0;
                sessionRequestLast = sessionRequestTotal = 0;
                byteCountLast = byteCountCurrent = 0;		    
            }
            Second second = new Second();
            // UPDATE SESSION GRAPH & COUNTERS
            if( MTransformDisplayJPanel.this.getUpdateSessions() ){
                sessionDynamicTimeSeriesCollection.getSeries(0).addOrUpdate(second, (float) sessionCountCurrent);
                sessionDynamicTimeSeriesCollection.getSeries(1).addOrUpdate(second, (float) sessionRequestTotal - sessionRequestLast);
                sessionRequestLast = sessionRequestTotal;
                generateCountLabel(sessionCountTotal, " ACC", sessionTotalJLabel);
                generateCountLabel(sessionRequestTotal, " REQ", sessionRequestTotalJLabel);
            }
            // UPDATE THROUGHPUT GRAPH & COUNTER
            if( MTransformDisplayJPanel.this.getUpdateThroughput() ){
                throughputDynamicTimeSeriesCollection.getSeries(0).addOrUpdate(second, ((float)(byteCountCurrent - byteCountLast))/1000f);
                byteCountLast = byteCountCurrent;
                generateCountLabel(byteCountCurrent, "B", throughputTotalJLabel);
            }
            // UPDATE BARS
            if( MTransformDisplayJPanel.this.getUpdateActivity() ){
                dataset.setValue( activity0Count.decayValue(), activitySeriesString, activityString0);
                dataset.setValue( activity1Count.decayValue(), activitySeriesString, activityString1);
                dataset.setValue( activity2Count.decayValue(), activitySeriesString, activityString2);
                dataset.setValue( activity3Count.decayValue(), activitySeriesString, activityString3);
            }
	    }
	}
    

	private void generateCountLabel(long count, String suffix, JLabel label){
	    if(count < 0 )
		label.setText("");
	    else if(count == 0){
		if(doVizUpdates)
		    label.setText( Long.toString( count ) + " " + suffix);
		else
		    label.setText("");
	    }
	    else if(count < 1000l)
		label.setText( Long.toString( count ) + " " + suffix);
	    else if(count < 1000000l)
		label.setText( Long.toString( count/1000l ) + "." + Util.padZero( count%1000l ) + " K" + suffix);
	    else if(count < 1000000000l)
		label.setText( Long.toString( count/1000000l ) + "." + Util.padZero( (count%1000000l)/1000l ) + " M" + suffix);
	    else if(count < 1000000000000l)
		label.setText( Long.toString( count/1000000000l ) + "." + Util.padZero( (count%1000000000l)/1000000l ) + " G" + suffix);
	    else if(count < 1000000000000000l)
		label.setText( Long.toString( count/1000000000000l ) + "." + Util.padZero( (count%1000000000000l)/1000000000l ) + " T" + suffix);
	    else if(count < 1000000000000000000l)
		label.setText( Long.toString( count/1000000000000000l ) + "." + Util.padZero( (count%1000000000000000l)/1000000000000l ) + " P" + suffix);
	    else
		label.setText( Long.toString( count/1000000000000000000l ) + "." + Util.padZero( (count%1000000000000000000l)/1000000000000000l ) + " E" + suffix);
	}
    
    }


    static class DoubleFIFO {

        static final double BARGRAPH_FALLOFF = .9d;

        double tempVal;

        double lastTotal;
        double windowTotal;
        int index;
        double data[];
        final int size;
        double decay;
        boolean resetable = false;
        DoubleFIFO(int size){
            data = new double[size];
            this.size = size;
            lastTotal = 0;
            windowTotal = 0;
            index = 0;
            decay = 0d;
            resetable = true;
            reset();
        }

        public synchronized void add(double newTotal){

            if(!resetable){
                lastTotal = newTotal;
            }
            else{
                tempVal = data[index];
                data[index] = newTotal - lastTotal;
                lastTotal = newTotal;
                windowTotal = windowTotal + data[index] - tempVal;
                index = (index + 1) % size;
            }
            resetable = true;
        }

        public synchronized double getTotal(){
            return windowTotal;
        }

        public synchronized void reset(){
            if(!resetable)
                return;
            for(int i=0; i<data.length; i++)
                data[i] = 0d;
            lastTotal = 0d;
            windowTotal = 0d;
            index = 0;
            decay = 0d;
            resetable = false;
        }

        public synchronized double lastValue(){
            return data[ (size + index-1) %size];
        }

        public synchronized double decayValue(){
            if(this.lastValue() > 0)
                decay = 98d;
            else
                decay *= BARGRAPH_FALLOFF;
            return decay;
        }
    }

    static class MVChartFactory
    {
        public static JFreeChart createBarChart(String title,
                                                String categoryAxisLabel,
                                                String valueAxisLabel,
                                                CategoryDataset dataset,
                                                PlotOrientation orientation,
                                                boolean legend,
                                                boolean tooltips,
                                                boolean urls) {

            if (orientation == null) {
                throw new IllegalArgumentException("Null 'orientation' argument.");
            }
            CategoryAxis categoryAxis = new CategoryAxis(categoryAxisLabel);
            ValueAxis valueAxis = new NumberAxis(valueAxisLabel);

            BarRenderer renderer = new BarRenderer();
            if (orientation == PlotOrientation.HORIZONTAL) {
                ItemLabelPosition position1 = new ItemLabelPosition(
                                                                    ItemLabelAnchor.OUTSIDE3, TextAnchor.CENTER_LEFT
                                                                    );
                renderer.setPositiveItemLabelPosition(position1);
                ItemLabelPosition position2 = new ItemLabelPosition(
                                                                    ItemLabelAnchor.OUTSIDE9, TextAnchor.CENTER_RIGHT
                                                                    );
                renderer.setNegativeItemLabelPosition(position2);
            }
            else if (orientation == PlotOrientation.VERTICAL) {
                ItemLabelPosition position1 = new ItemLabelPosition(
                                                                    ItemLabelAnchor.OUTSIDE12, TextAnchor.BOTTOM_CENTER
                                                                    );
                renderer.setPositiveItemLabelPosition(position1);
                ItemLabelPosition position2 = new ItemLabelPosition(
                                                                    ItemLabelAnchor.OUTSIDE6, TextAnchor.TOP_CENTER
                                                                    );
                renderer.setNegativeItemLabelPosition(position2);
            }
            if (tooltips) {
                renderer.setToolTipGenerator(new StandardCategoryToolTipGenerator());
            }
            if (urls) {
                renderer.setItemURLGenerator(new StandardCategoryURLGenerator());
            }

            CategoryPlot plot = new CategoryPlot(dataset, categoryAxis, valueAxis, renderer);
            plot.setOrientation(orientation);
            JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, legend);

            return chart;

        }

        public static JFreeChart createXYLineChart(String title,
                                                   String xAxisLabel,
                                                   String yAxisLabel,
                                                   XYDataset dataset,
                                                   PlotOrientation orientation,
                                                   boolean legend,
                                                   boolean tooltips,
                                                   boolean urls) {

            if (orientation == null) {
                throw new IllegalArgumentException("Null 'orientation' argument.");
            }
            NumberAxis xAxis = new NumberAxis(xAxisLabel);
            xAxis.setAutoRangeIncludesZero(false);
            NumberAxis yAxis = new NumberAxis(yAxisLabel);
            XYItemRenderer renderer = new StandardXYItemRenderer(StandardXYItemRenderer.LINES);
            XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer);
            plot.setOrientation(orientation);
            if (tooltips) {
                renderer.setToolTipGenerator(new StandardXYToolTipGenerator());
            }
            if (urls) {
                renderer.setURLGenerator(new StandardXYURLGenerator());
            }

            JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, legend);

            return chart;

        }
    }
}
