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

package com.metavize.gui.install;

import com.metavize.gui.widgets.wizard.*;

import javax.swing.SwingUtilities;
import javax.swing.JProgressBar;
import java.awt.Color;


public class InstallBenchmarkJPanel extends MWizardPageJPanel {

    private static final int MEMORY_MIN_MEGS  = 1024;
    private static final int MEMORY_GOOD_MEGS = 2048;
    private static final int CPU_MIN_MHZ  = 1000;
    private static final int CPU_GOOD_MHZ = 2000;
    private static final int DISK_MIN_GIGS  = 40;
    private static final int DISK_GOOD_GIGS = 60;
    private static final int NICS_MIN_COUNT  = 2;
    private static final int NICS_GOOD_COUNT = 3;
    
    public InstallBenchmarkJPanel() {
        initComponents();
    }

    protected void initialFocus(){
	new DetectThread();
    }

    class DetectThread extends Thread {
	public void DetectThread(){
	    setDaemon(true);
	    start();
	}
	public void run(){
	    System.out.println("HERE");
	    try{
		InstallWizard.getInfiniteProgressJComponent().startLater("Checking...");
		SwingUtilities.invokeLater( new Runnable(){ public void run() {
		    updateJProgressBar(memoryJProgressBar, "Checking...", true, 0, 68, 91, 255);
		    updateJProgressBar(cpuJProgressBar, "Checking...", true, 0, 68, 91, 255);
		    updateJProgressBar(diskJProgressBar, "Checking...", true, 0, 68, 91, 255);
		    updateJProgressBar(nicJProgressBar, "Checking...", true, 0, 68, 91, 255);
		    memoryResultJLabel.setText("undetermined");
		    cpuResultJLabel.setText("undetermined");
		    diskResultJLabel.setText("undetermined");
		    nicResultJLabel.setText("undetermined");
		}});
		
		// RUN TEST
		final int memory   = SystemStats.getMemoryMegs();
		final int cpuCount = SystemStats.getLogicalCPU();
		final int cpuSpeed = SystemStats.getClockSpeed();
		final int disk     = SystemStats.getDiskGigs();
		final int nics     = SystemStats.getNumNICs();
		// COMPUTE RESULTS
		final Object memoryResults[] = computeResults(memory,MEMORY_MIN_MEGS,MEMORY_GOOD_MEGS);
		final Object cpuResults[]    = computeResults(cpuSpeed,CPU_MIN_MHZ,CPU_GOOD_MHZ);
		final Object diskResults[]   = computeResults(disk,DISK_MIN_GIGS,DISK_GOOD_GIGS);
		final Object nicsResults[]   = computeResults(nics,NICS_MIN_COUNT,NICS_GOOD_COUNT);
		
		SwingUtilities.invokeLater( new Runnable(){ public void run() {
		    updateJProgressBar(memoryJProgressBar, (String)memoryResults[0], false, (Integer)memoryResults[1],
				       (Integer)memoryResults[2], (Integer)memoryResults[3], (Integer)memoryResults[4]);
		    updateJProgressBar(cpuJProgressBar, (String)cpuResults[0], false, (Integer)cpuResults[1],
				       (Integer)cpuResults[2], (Integer)cpuResults[3], (Integer)cpuResults[4]);
		    updateJProgressBar(diskJProgressBar, (String)diskResults[0], false, (Integer)diskResults[1],
				       (Integer)diskResults[2], (Integer)diskResults[3], (Integer)diskResults[4]);
		    updateJProgressBar(nicJProgressBar, (String)nicsResults[0], false, (Integer)nicsResults[1],
				       (Integer)nicsResults[2], (Integer)nicsResults[3], (Integer)nicsResults[4]);
		    
		    memoryResultJLabel.setText(memory + " MB");
		    cpuResultJLabel.setText(cpuSpeed + " MHz" + (cpuCount>1?" (x"+cpuCount+")":""));
		    diskResultJLabel.setText(disk + " GB");
		    nicResultJLabel.setText(nics + " interface" + (nics>1?"s":""));
		}});
		InstallWizard.getInfiniteProgressJComponent().stopLater(4000l);
	    }
	    catch(Exception e){
		InstallWizard.getInfiniteProgressJComponent().stopLater(-1);
		e.printStackTrace();
	    }
	}
    }

    private void updateJProgressBar(JProgressBar jProgressBar, String message, boolean indeterminate, int value, int r, int g, int b){
	jProgressBar.setString(message);
	jProgressBar.setIndeterminate(indeterminate);
	jProgressBar.setValue(value);
	jProgressBar.setForeground(new Color(r,g,b));
    }

    private Object[] computeResults(Integer score, Integer min, Integer max){
	String resultString;
	Integer resultValue, resultR, resultG, resultB;
	if( score <= min ){
	    resultString = "Test Failed";
	    resultValue  = 33;
	    resultR = 255;
	    resultG = resultB = 0;
	}
	else if(score <= max ){
	    resultString = "Warning";
	    resultValue = 66;
	    resultR = resultG = 255;
	    resultB = 0;
	}
	else{
	    resultString = "Test Passed";
	    resultValue = 100;
	    resultG = 255;
	    resultR = resultB = 0;
	}
	return new Object[]{resultString,resultValue,resultR,resultG,resultB};
    }

        private void initComponents() {//GEN-BEGIN:initComponents
                java.awt.GridBagConstraints gridBagConstraints;

                contentJPanel = new javax.swing.JPanel();
                jLabel1 = new javax.swing.JLabel();
                memoryJPanel = new javax.swing.JPanel();
                memoryJProgressBar = new javax.swing.JProgressBar();
                memoryNameJLabel = new javax.swing.JLabel();
                memoryResultJLabel = new javax.swing.JLabel();
                cpuJPanel = new javax.swing.JPanel();
                cpuJProgressBar = new javax.swing.JProgressBar();
                cpuNameJLabel = new javax.swing.JLabel();
                cpuResultJLabel = new javax.swing.JLabel();
                diskJPanel = new javax.swing.JPanel();
                diskJProgressBar = new javax.swing.JProgressBar();
                diskNameJLabel = new javax.swing.JLabel();
                diskResultJLabel = new javax.swing.JLabel();
                nicJPanel = new javax.swing.JPanel();
                nicJProgressBar = new javax.swing.JProgressBar();
                nicNameJLabel = new javax.swing.JLabel();
                nicResultJLabel = new javax.swing.JLabel();
                resultJLabel = new javax.swing.JLabel();
                backgroundJPabel = new javax.swing.JLabel();

                setLayout(new java.awt.GridBagLayout());

                setOpaque(false);
                contentJPanel.setLayout(new java.awt.GridBagLayout());

                contentJPanel.setOpaque(false);
                jLabel1.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel1.setText("<html>These tests show you if your computer meets minimum hardware requirements.</html>");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(15, 15, 0, 15);
                contentJPanel.add(jLabel1, gridBagConstraints);

                memoryJPanel.setLayout(new java.awt.GridBagLayout());

                memoryJPanel.setOpaque(false);
                memoryJProgressBar.setFont(new java.awt.Font("Dialog", 0, 12));
                memoryJProgressBar.setForeground(new java.awt.Color(68, 91, 255));
                memoryJProgressBar.setValue(25);
                memoryJProgressBar.setFocusable(false);
                memoryJProgressBar.setMaximumSize(new java.awt.Dimension(32767, 16));
                memoryJProgressBar.setMinimumSize(new java.awt.Dimension(10, 16));
                memoryJProgressBar.setPreferredSize(new java.awt.Dimension(148, 16));
                memoryJProgressBar.setRequestFocusEnabled(false);
                memoryJProgressBar.setString("Memory Test");
                memoryJProgressBar.setStringPainted(true);
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.gridwidth = 2;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                memoryJPanel.add(memoryJProgressBar, gridBagConstraints);

                memoryNameJLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
                memoryNameJLabel.setText("Main Memory:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
                memoryJPanel.add(memoryNameJLabel, gridBagConstraints);

                memoryResultJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                memoryResultJLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
                memoryResultJLabel.setText("1024 MB");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                memoryJPanel.add(memoryResultJLabel, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.insets = new java.awt.Insets(30, 15, 0, 15);
                contentJPanel.add(memoryJPanel, gridBagConstraints);

                cpuJPanel.setLayout(new java.awt.GridBagLayout());

                cpuJPanel.setOpaque(false);
                cpuJProgressBar.setFont(new java.awt.Font("Dialog", 0, 12));
                cpuJProgressBar.setForeground(new java.awt.Color(68, 91, 255));
                cpuJProgressBar.setValue(25);
                cpuJProgressBar.setFocusable(false);
                cpuJProgressBar.setMaximumSize(new java.awt.Dimension(32767, 16));
                cpuJProgressBar.setMinimumSize(new java.awt.Dimension(10, 16));
                cpuJProgressBar.setPreferredSize(new java.awt.Dimension(148, 16));
                cpuJProgressBar.setRequestFocusEnabled(false);
                cpuJProgressBar.setString("CPU Test");
                cpuJProgressBar.setStringPainted(true);
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.gridwidth = 2;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                cpuJPanel.add(cpuJProgressBar, gridBagConstraints);

                cpuNameJLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
                cpuNameJLabel.setText("CPU:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
                cpuJPanel.add(cpuNameJLabel, gridBagConstraints);

                cpuResultJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                cpuResultJLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
                cpuResultJLabel.setText("1.5Ghz (2x)");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                cpuJPanel.add(cpuResultJLabel, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.insets = new java.awt.Insets(20, 15, 0, 15);
                contentJPanel.add(cpuJPanel, gridBagConstraints);

                diskJPanel.setLayout(new java.awt.GridBagLayout());

                diskJPanel.setOpaque(false);
                diskJProgressBar.setFont(new java.awt.Font("Dialog", 0, 12));
                diskJProgressBar.setForeground(new java.awt.Color(68, 91, 255));
                diskJProgressBar.setValue(25);
                diskJProgressBar.setFocusable(false);
                diskJProgressBar.setMaximumSize(new java.awt.Dimension(32767, 16));
                diskJProgressBar.setMinimumSize(new java.awt.Dimension(10, 16));
                diskJProgressBar.setPreferredSize(new java.awt.Dimension(148, 16));
                diskJProgressBar.setRequestFocusEnabled(false);
                diskJProgressBar.setString("Hard Disk Test");
                diskJProgressBar.setStringPainted(true);
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.gridwidth = 2;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                diskJPanel.add(diskJProgressBar, gridBagConstraints);

                diskNameJLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
                diskNameJLabel.setText("Hard Disk:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
                diskJPanel.add(diskNameJLabel, gridBagConstraints);

                diskResultJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                diskResultJLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
                diskResultJLabel.setText("45 GB");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 0.5;
                diskJPanel.add(diskResultJLabel, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.insets = new java.awt.Insets(20, 15, 0, 15);
                contentJPanel.add(diskJPanel, gridBagConstraints);

                nicJPanel.setLayout(new java.awt.GridBagLayout());

                nicJPanel.setOpaque(false);
                nicJProgressBar.setFont(new java.awt.Font("Dialog", 0, 12));
                nicJProgressBar.setForeground(new java.awt.Color(68, 91, 255));
                nicJProgressBar.setValue(25);
                nicJProgressBar.setFocusable(false);
                nicJProgressBar.setMaximumSize(new java.awt.Dimension(32767, 16));
                nicJProgressBar.setMinimumSize(new java.awt.Dimension(10, 16));
                nicJProgressBar.setPreferredSize(new java.awt.Dimension(148, 16));
                nicJProgressBar.setRequestFocusEnabled(false);
                nicJProgressBar.setString("Network Interface Test");
                nicJProgressBar.setStringPainted(true);
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.gridwidth = 2;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                nicJPanel.add(nicJProgressBar, gridBagConstraints);

                nicNameJLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
                nicNameJLabel.setText("Network Interfaces:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
                nicJPanel.add(nicNameJLabel, gridBagConstraints);

                nicResultJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                nicResultJLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
                nicResultJLabel.setText("3 Interfaces");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 0.5;
                nicJPanel.add(nicResultJLabel, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.insets = new java.awt.Insets(20, 15, 0, 15);
                contentJPanel.add(nicJPanel, gridBagConstraints);

                resultJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                resultJLabel.setText("This is where the resule message goes...");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(15, 15, 0, 15);
                contentJPanel.add(resultJLabel, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                add(contentJPanel, gridBagConstraints);

                backgroundJPabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/metavize/gui/install/ProductShot.png")));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
                gridBagConstraints.weightx = 1.0;
                add(backgroundJPabel, gridBagConstraints);

        }//GEN-END:initComponents
    
    
        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JLabel backgroundJPabel;
        private javax.swing.JPanel contentJPanel;
        private javax.swing.JPanel cpuJPanel;
        private javax.swing.JProgressBar cpuJProgressBar;
        private javax.swing.JLabel cpuNameJLabel;
        private javax.swing.JLabel cpuResultJLabel;
        private javax.swing.JPanel diskJPanel;
        private javax.swing.JProgressBar diskJProgressBar;
        private javax.swing.JLabel diskNameJLabel;
        private javax.swing.JLabel diskResultJLabel;
        private javax.swing.JLabel jLabel1;
        private javax.swing.JPanel memoryJPanel;
        private javax.swing.JProgressBar memoryJProgressBar;
        private javax.swing.JLabel memoryNameJLabel;
        private javax.swing.JLabel memoryResultJLabel;
        private javax.swing.JPanel nicJPanel;
        private javax.swing.JProgressBar nicJProgressBar;
        private javax.swing.JLabel nicNameJLabel;
        private javax.swing.JLabel nicResultJLabel;
        private javax.swing.JLabel resultJLabel;
        // End of variables declaration//GEN-END:variables
    
}
