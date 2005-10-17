/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.gui.transform;

import com.metavize.gui.util.*;
import com.metavize.gui.widgets.MMultilineToolTip;
import com.metavize.mvvm.security.Tid;
import com.metavize.mvvm.tran.TransformContext;
import com.metavize.mvvm.tran.*;

import java.awt.*;
import java.text.DecimalFormat;
import javax.swing.*;
import java.util.*;

import org.jfree.chart.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.plot.*;
import org.jfree.data.*;
import org.jfree.data.xy.*;
import org.jfree.data.statistics.*;
import org.jfree.data.time.*;
import org.jfree.data.category.*;



public class MTransformDisplayJPanel extends javax.swing.JPanel {
        
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
    protected AreaDynamicTimeSeriesCollection throughputDynamicTimeSeriesCollection;
    protected ChartPanel sessionChartJPanel;
    protected AreaDynamicTimeSeriesCollection sessionDynamicTimeSeriesCollection;

    // ACTIVITY COUNT DISPLAY
    protected ChartPanel activityChartJPanel;
    private static String activitySeriesString = "activitySeries";  // row key
    private static String activityString0 = "Activity 0";  // row key
    private static String activityString1 = "Activity 1";  // row key
    private static String activityString2 = "Activity 2";  // row key
    private static String activityString3 = "Activity 3";  // row key
    private DefaultCategoryDataset dataset = new DefaultCategoryDataset();

    
    public MTransformDisplayJPanel(MTransformJPanel mTransformJPanel) {
        this.mTransformJPanel = mTransformJPanel;
        
        throughputDynamicTimeSeriesCollection = new AreaDynamicTimeSeriesCollection(1, 60, new Second());
        throughputDynamicTimeSeriesCollection.setTimeBase(new Second());
        throughputChartJPanel = createLineChart(throughputDynamicTimeSeriesCollection, true); 

        sessionDynamicTimeSeriesCollection = new AreaDynamicTimeSeriesCollection(2, 60, new Second());
        sessionDynamicTimeSeriesCollection.setTimeBase(new Second());
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
    }
    
    void doShutdown(){
	updateGraphThread.kill();
    }

    public void setUpdateGraph(boolean updateGraph){
	updateGraphThread.setUpdateGraph( updateGraph );
    }
    
    private ChartPanel createBarChart(CategoryDataset dataset) {    
        
        JFreeChart jFreeChart = ChartFactory.createBarChart(null,null,null, dataset, PlotOrientation.HORIZONTAL, false,false,false);
        jFreeChart.setBackgroundPaint(Color.BLACK);
        jFreeChart.setBorderPaint(null);
        jFreeChart.setBorderVisible(false);
        
        // PLOT
        CategoryPlot plot = jFreeChart.getCategoryPlot();
        plot.setBackgroundPaint(Color.BLACK);
        plot.setOutlinePaint(null);
        plot.getRenderer().setSeriesPaint(0, new Color(.4f, .4f, 1f, .5f));
        plot.getRenderer().setSeriesOutlinePaint(0, new Color(.4f, .4f, 1f, 1f));
        plot.setInsets(new java.awt.Insets(0,0,0,2), false);
        plot.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
        plot.setDomainGridlinesVisible(false);
        plot.setDomainGridlinePaint(null);
        plot.setRangeGridlinesVisible(false);
        plot.setRangeGridlinePaint(null);
        
        // X AXIS
        CategoryAxis catAxis = plot.getDomainAxis();
        catAxis.setVisible(false);
        catAxis.setAxisLineVisible(true);
        catAxis.setAxisLinePaint(Color.BLACK);
        //catAxis.setTickLabelPaint(null);
        //catAxis.setTickMarkPaint(null);
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
 
        return chartPanel;
    }
        
    
    private ChartPanel createLineChart(TableXYDataset dataset, boolean showDecimal) {    
        
        JFreeChart jFreeChart = ChartFactory.createXYLineChart(null,null,null, dataset, PlotOrientation.VERTICAL, false,false,false);
        jFreeChart.setBackgroundPaint(Color.BLACK);
        XYPlot plot = jFreeChart.getXYPlot();
        plot.setBackgroundPaint(Color.BLACK);
        plot.setOutlinePaint(null);

        plot.getRenderer().setSeriesPaint(0, new Color(.5f, .5f, 1f));
        plot.getRenderer().setSeriesPaint(1, new Color(.5f, .25f, .5f));
        

        plot.setInsets(new java.awt.Insets(4,0,6,2), false);
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

    
    private class UpdateGraphThread extends Thread implements Killable {

	// GRAPH CONTROL //////
	private boolean killed;
	public synchronized void kill(){
	    killed = true;
	    notify();
	}
	private boolean updateGraph;
	public synchronized void setUpdateGraph(boolean updateGraph){ 
	    this.updateGraph = updateGraph;
	    if( updateGraph )
		notify();
	}
	//////////////////////

        TransformStats currentStats = null;
        long sessionCountCurrent = 0;
        long sessionCountTotal = 0;
        long sessionRequestCurrent = 0;
        long sessionRequestLast = 0;
        long byteCountLast = 0;
        long byteCountCurrent = 0;

        double sessionFactor;
        double throughputFactor;
        DoubleFIFO activity0Count = new DoubleFIFO(60);
        DoubleFIFO activity1Count = new DoubleFIFO(60);
        DoubleFIFO activity2Count = new DoubleFIFO(60);
        DoubleFIFO activity3Count = new DoubleFIFO(60);
        
        int newestIndex;
        double activityTotal;
        
        public UpdateGraphThread(){
	    super("MVCLIENT-UpdateGraphThread: " + MTransformDisplayJPanel.this.mTransformJPanel.getMackageDesc().getDisplayName());
            this.setDaemon(true);
	    Util.addKillableThread(this);
            resetCounters();
	    this.start();
        }

        private UpdateGraphRunnable updateGraphRunnable = new UpdateGraphRunnable();
	private void doUpdateGraph(){
	    SwingUtilities.invokeLater( updateGraphRunnable );
	}
	private class UpdateGraphRunnable implements Runnable {
	    public void run(){
		if(!updateGraph){
		    resetCounters();
		    for(int i=0; i<60; i++){
			newestIndex = sessionDynamicTimeSeriesCollection.getNewestIndex();
			sessionDynamicTimeSeriesCollection.addValue(0, newestIndex, 0f);
			sessionDynamicTimeSeriesCollection.addValue(1, newestIndex, 0f);
			sessionDynamicTimeSeriesCollection.advanceTime();
			throughputDynamicTimeSeriesCollection.addValue( 0, throughputDynamicTimeSeriesCollection.getNewestIndex(), 0f);
			throughputDynamicTimeSeriesCollection.advanceTime();
		    }
		    byteCountLast = byteCountCurrent = 0;
		    sessionRequestLast = sessionRequestCurrent = 0;
		}
		if( MTransformDisplayJPanel.this.getUpdateSessions() ){
		    newestIndex = sessionDynamicTimeSeriesCollection.getNewestIndex();
		    sessionDynamicTimeSeriesCollection.addValue(0, newestIndex, (float) sessionCountCurrent);
		    sessionDynamicTimeSeriesCollection.addValue(1, newestIndex, (float) sessionRequestCurrent - sessionRequestLast);
		    sessionDynamicTimeSeriesCollection.advanceTime();
		    sessionRequestLast = sessionRequestCurrent;
		    generateCountLabel(sessionCountTotal, " ACC", sessionTotalJLabel);
		    generateCountLabel(sessionRequestCurrent, " REQ", sessionRequestTotalJLabel);
		}		    
		if( MTransformDisplayJPanel.this.getUpdateThroughput() ){
		    throughputDynamicTimeSeriesCollection.addValue( 0, 
								    throughputDynamicTimeSeriesCollection.getNewestIndex(), 
								    ((float)(byteCountCurrent - byteCountLast))/1000f);
		    throughputDynamicTimeSeriesCollection.advanceTime();
		    byteCountLast = byteCountCurrent;
		    generateCountLabel(byteCountCurrent, "B", throughputTotalJLabel);
		}		    
		if( MTransformDisplayJPanel.this.getUpdateActivity() ){
		    dataset.setValue( activity0Count.decayValue(), activitySeriesString, activityString0);
		    dataset.setValue( activity1Count.decayValue(), activitySeriesString, activityString1);
		    dataset.setValue( activity2Count.decayValue(), activitySeriesString, activityString2);
		    dataset.setValue( activity3Count.decayValue(), activitySeriesString, activityString3);
		}
	    }
	}
        
        
        private void generateCountLabel(long count, String suffix, JLabel label){
            if(count < 1000l)
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
    
        private void resetCounters(){
            activity0Count.reset();
            activity1Count.reset();
            activity2Count.reset();
            activity3Count.reset();
        }
    
        public void run() {
            while(true){
                try{
                    // GET TRANSFORM STATS AND HANDLE KILL/PAUSE
		    synchronized(this){			
			if( killed ){
			    return;
			}
			if( !updateGraph ){
			    doUpdateGraph();
			    wait();
			}
			if( killed ){
			    return;
			}
			currentStats = Util.getStatsCache().getFakeTransform(mTransformJPanel.getTid()).getStats();
		    }

		    // UPDATE COUNTS
                    sessionCountCurrent = currentStats.tcpSessionCount()
                                        + currentStats.udpSessionCount();
                    sessionCountTotal = currentStats.tcpSessionTotal()
                                      + currentStats.udpSessionTotal();
                    sessionRequestCurrent = currentStats.tcpSessionRequestTotal()
                                          + currentStats.udpSessionRequestTotal();
                    byteCountCurrent = currentStats.c2tBytes()
                                     + currentStats.s2tBytes();

		    // RESET COUNTS IF NECESSARY
                    if( (byteCountLast == 0) || (byteCountLast > byteCountCurrent) )
                        byteCountLast = byteCountCurrent;
                    if( (sessionRequestLast == 0) || (sessionRequestLast > sessionRequestCurrent) )
                        sessionRequestLast = sessionRequestCurrent;

		    // ADD TO COUNTS
                    activity0Count.add( (double) currentStats.getCount(Transform.GENERIC_0_COUNTER) );                    
                    activity1Count.add( (double) currentStats.getCount(Transform.GENERIC_1_COUNTER) );
                    activity2Count.add( (double) currentStats.getCount(Transform.GENERIC_2_COUNTER) );
                    activity3Count.add( (double) currentStats.getCount(Transform.GENERIC_3_COUNTER) );

                    // UPDATE GRAPHS
                    doUpdateGraph();

                    // PAUSE A NORMAL AMOUNT OF TIME
                    Thread.sleep(SLEEP_MILLIS);     
                }
                catch(Exception e){  // handle this exception much more gracefully
		    try{ Thread.currentThread().sleep(10000); } catch(Exception f){}
                }
            }
        }
        
    }
    

    
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
    private javax.swing.JLabel throughputJLabel;
    protected javax.swing.JLabel throughputTotalJLabel;
    // End of variables declaration//GEN-END:variables
    
}

class DoubleFIFO {
    
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

class AreaDynamicTimeSeriesCollection extends DynamicTimeSeriesCollection implements TableXYDataset {
    
    AreaDynamicTimeSeriesCollection(int nSeries, int nMoments){ super(nSeries, nMoments); }
    AreaDynamicTimeSeriesCollection(int nSeries, int nMoments, RegularTimePeriod timeSample){ super(nSeries, nMoments, timeSample); }
    AreaDynamicTimeSeriesCollection(int nSeries, int nMoments, RegularTimePeriod timeSample, java.util.TimeZone zone){ super(nSeries, nMoments, timeSample, zone); }
    AreaDynamicTimeSeriesCollection(int nSeries, int nMoments, java.util.TimeZone zone){ super(nSeries, nMoments, zone); }
    
    public int getItemCount() {
        return super.getItemCount(0);
    }
    
}
