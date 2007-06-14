/*
 * $HeadURL:$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.gui.install;

import java.awt.Color;
import java.text.DecimalFormat;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.widgets.wizard.*;

public class InstallBenchmarkJPanel extends MWizardPageJPanel {

    private static final String FAILURE_MESSAGE = "<html><font color=\"#FF0000\"><b>Warning!</b><br>"
        + "Your hardware does not meet minimum requirements. This system may not function properly.<br>"
        + "You may continue installation, but it is recommended that you upgrade your system hardware.<br><br>"
        + "Press the Next button in the main window to continue.</font></html>";
    private static final String WARNING_MESSAGE = "<html><font color=\"#FF9B00\"><b>Warning!</b><br>"
        + "Your hardware meets minimum requirements, but performance may not be optimal.<br>"
        + "Press the Next button in the main window to continue.</font></html>";
    private static final String PASSED_MESSAGE = "<html><font color=\"#445BFF\"><b>Success!</b><br>"
        + "Your hardware meets all requirements.<br>"
        + "Press the Next button in the main window to continue.</font></html>";

    private static final int MEMORY_MIN_MEGS   = 475;
    private static final int MEMORY_GOOD_MEGS  = 1000;
    private static final int MEMORY_GREAT_MEGS = 2000;
    private static final String MEMORY_REQUIRED = "("+MEMORY_MIN_MEGS+" MB required)";

    private static final int CPU_MIN_MHZ   = 750;
    private static final int CPU_GOOD_MHZ  = 1600;
    private static final int CPU_GREAT_MHZ = 3000;
    private static final String CPU_REQUIRED = "("+CPU_MIN_MHZ+" MHz required)";

    private static final int DISK_MIN_GIGS   = 20;
    private static final int DISK_GOOD_GIGS  = 40;
    private static final int DISK_GREAT_GIGS = 80;
    private static final String DISK_REQUIRED = "("+DISK_MIN_GIGS+" GB required)";

    private static final int NICS_MIN_COUNT   = 2;
    private static final int NICS_GOOD_COUNT  = 2;
    private static final int NICS_GREAT_COUNT = 3;
    private static final String NICS_REQUIRED = "("+NICS_MIN_COUNT+" interfaces required)";

    private boolean testPassed = false;
    private InstallWizard installWizard;

    public InstallBenchmarkJPanel(InstallWizard installWizard) {
        this.installWizard = installWizard;
        initComponents();
    }

    protected void initialFocus(){
        new DetectThread();
    }

    protected boolean leavingForwards(){
        if(!testPassed && !installWizard.isShiftDown())
            System.exit(1);
        return true;
    }

    class DetectThread extends Thread {
        public DetectThread(){
            setDaemon(true);
            installWizard.updateButtonState(true);
            updateJProgressBar(memoryJProgressBar, "Checking...", true, 0, 68, 91, 255);
            updateJProgressBar(cpuJProgressBar, "Checking...", true, 0, 68, 91, 255);
            updateJProgressBar(diskJProgressBar, "Checking...", true, 0, 68, 91, 255);
            updateJProgressBar(nicJProgressBar, "Checking...", true, 0, 68, 91, 255);
            memoryResultJLabel.setText("undetermined ");
            cpuResultJLabel.setText("undetermined");
            diskResultJLabel.setText("undetermined");
            nicResultJLabel.setText("undetermined");
            resultJLabel.setText("");
            start();
        }
        public void run(){
            try{
                // RUN TEST
                final int memory   = SystemStats.getMemoryMegs();
                final int cpuCount = SystemStats.getLogicalCPU();
                final int cpuSpeed = SystemStats.getClockSpeed();
                float cpuMultiplier;
                if( cpuCount == 1 )
                    cpuMultiplier = 1f;
                else if( cpuCount == 2 )
                    cpuMultiplier = 1.5f;
                else if( cpuCount == 4 )
                    cpuMultiplier = 2.5f;
                else
                    cpuMultiplier = 3f;
                final int logicalCpuSpeed = (int)((float)cpuSpeed * cpuMultiplier);
                final float disk   = SystemStats.getDiskGigs(InstallWizard.getTargetDisk());
                final int nics     = SystemStats.getNumNICs();
                // COMPUTE RESULTS
                final Object memoryResults[] = computeNormalResults((float)memory,
                                                                    MEMORY_MIN_MEGS,MEMORY_GOOD_MEGS,MEMORY_GREAT_MEGS,
                                                                    MEMORY_REQUIRED);
                final Object cpuResults[]    = computeNormalResults((float)logicalCpuSpeed,
                                                                    CPU_MIN_MHZ,CPU_GOOD_MHZ,CPU_GREAT_MHZ,
                                                                    CPU_REQUIRED);
                final Object diskResults[]   = computeNormalResults(disk,
                                                                    DISK_MIN_GIGS,DISK_GOOD_GIGS,DISK_GREAT_GIGS,
                                                                    DISK_REQUIRED);
                final Object nicsResults[]   = computeNicResults((float)nics,
                                                                 NICS_MIN_COUNT,NICS_GOOD_COUNT,NICS_GREAT_COUNT,
                                                                 NICS_REQUIRED);

                sleep(3000l);


                SwingUtilities.invokeAndWait( new Runnable(){ public void run() {
                    installWizard.updateButtonState(false);
                    updateJProgressBar(memoryJProgressBar, (String)memoryResults[0], false, (Integer)memoryResults[1],
                                       (Integer)memoryResults[2], (Integer)memoryResults[3], (Integer)memoryResults[4]);
                    updateJProgressBar(cpuJProgressBar, (String)cpuResults[0], false, (Integer)cpuResults[1],
                                       (Integer)cpuResults[2], (Integer)cpuResults[3], (Integer)cpuResults[4]);
                    updateJProgressBar(diskJProgressBar, (String)diskResults[0], false, (Integer)diskResults[1],
                                       (Integer)diskResults[2], (Integer)diskResults[3], (Integer)diskResults[4]);
                    updateJProgressBar(nicJProgressBar, (String)nicsResults[0], false, (Integer)nicsResults[1],
                                       (Integer)nicsResults[2], (Integer)nicsResults[3], (Integer)nicsResults[4]);

                    memoryResultJLabel.setText(memory + " MB");
                    cpuResultJLabel.setText(cpuSpeed + " MHz" + " ("+cpuCount+" detected)"
                                            + "  [approximated as a single " + logicalCpuSpeed + " MHz CPU]");
                    DecimalFormat decimalFormat = new DecimalFormat("#####.##");
                    diskResultJLabel.setText(decimalFormat.format(disk) + " GB");
                    nicResultJLabel.setText(nics + " interface" + (nics==1?"":"s"));
                }});
                String resultMessage;
                if(((Integer)memoryResults[1]<25) || ((Integer)cpuResults[1]<25)
                   || ((Integer)diskResults[1]<25) || ((Integer)nicsResults[1]<=25)){
                    resultMessage = FAILURE_MESSAGE;
                    //resultJLabel.setText(FAILURE_MESSAGE);
                    testPassed = true;
                }
                else if(((Integer)memoryResults[1]<50) || ((Integer)cpuResults[1]<50)
                        || ((Integer)diskResults[1]<50)){
                    resultMessage = WARNING_MESSAGE;
                    //resultJLabel.setText(WARNING_MESSAGE);
                    testPassed = true;
                }
                else{
                    resultMessage = PASSED_MESSAGE;
                    //resultJLabel.setText(PASSED_MESSAGE);
                    testPassed = true;
                }
                MOneButtonJDialog dialog = MOneButtonJDialog.factory(InstallBenchmarkJPanel.this.getTopLevelAncestor(), "Install Wizard", resultMessage,
                                                                     "Install Wizard", "Result");
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    private void updateJProgressBar(JProgressBar jProgressBar, String message, boolean indeterminate,
                                    int value, int r, int g, int b){
        jProgressBar.setString(message);
        jProgressBar.setIndeterminate(indeterminate);
        jProgressBar.setValue(value);
        jProgressBar.setForeground(new Color(r,g,b));
    }

    private Object[] computeNormalResults(Float score, Integer min, Integer good, Integer great, String failureString){
        String resultString;
        Integer resultValue, resultR, resultG, resultB;
        if( score < min ){
            resultString = "Failure " + failureString;
            resultValue  = (int) (25f*(score/((float)min)));
            resultR = 255;
            resultG = resultB = 0;
        }
        else if(score < good ){
            resultString = "Warning";
            resultValue = 25 + (int) (25f*((score-(float)min)/((float)good-(float)min)));
            resultR = 255;
            resultG = 155;
            resultB = 0;
        }
        else if(score < great){
            resultString = "Good";
            resultValue = 50 + (int) (25f*((score-(float)good)/((float)great-(float)good)));
            resultG = 255;
            resultR = resultB = 0;
        }
        else if(score == (float)great){
            resultString = "Excellent";
            resultValue = 75;
            resultG = 255;
            resultR = resultB = 0;
        }
        else{
            resultString = "Excellent";
            resultValue = 95;
            resultG = 255;
            resultR = resultB = 0;
        }
        return new Object[]{resultString,resultValue,resultR,resultG,resultB};
    }

    private Object[] computeNicResults(Float score, Integer min, Integer good, Integer great, String failureString){
        String resultString;
        Integer resultValue, resultR, resultG, resultB;
        if( score < min ){
            resultString = "Failure " + failureString;
            resultValue  = 25;
            resultR = 255;
            resultG = resultB = 0;
        }
        else if(score < good ){
            resultString = "Warning";
            resultValue = 50;
            resultR = 255;
            resultG = 155;
            resultB = 0;
        }
        else if(score < great){
            resultString = "Good";
            resultValue = 50;
            resultG = 255;
            resultR = resultB = 0;
        }
        else if(score == (float)great){
            resultString = "Excellent";
            resultValue = 75;
            resultG = 255;
            resultR = resultB = 0;
        }
        else{
            resultString = "Excellent";
            resultValue = 95;
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
        memoryNameJLabel = new javax.swing.JLabel();
        memoryResultJLabel = new javax.swing.JLabel();
        memoryJProgressBar = new javax.swing.JProgressBar();
        memoryLabelJLabel = new javax.swing.JLabel();
        cpuJPanel = new javax.swing.JPanel();
        cpuNameJLabel = new javax.swing.JLabel();
        cpuResultJLabel = new javax.swing.JLabel();
        cpuJProgressBar = new javax.swing.JProgressBar();
        cpuLabelJLabel = new javax.swing.JLabel();
        diskJPanel = new javax.swing.JPanel();
        diskNameJLabel = new javax.swing.JLabel();
        diskResultJLabel = new javax.swing.JLabel();
        diskJProgressBar = new javax.swing.JProgressBar();
        diskLabelJLabel = new javax.swing.JLabel();
        nicJPanel = new javax.swing.JPanel();
        nicNameJLabel = new javax.swing.JLabel();
        nicResultJLabel = new javax.swing.JLabel();
        nicJProgressBar = new javax.swing.JProgressBar();
        nicLabelJLabel = new javax.swing.JLabel();
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

        memoryJProgressBar.setFont(new java.awt.Font("Dialog", 0, 12));
        memoryJProgressBar.setForeground(new java.awt.Color(68, 91, 255));
        memoryJProgressBar.setValue(25);
        memoryJProgressBar.setFocusable(false);
        memoryJProgressBar.setMaximumSize(new java.awt.Dimension(480, 20));
        memoryJProgressBar.setMinimumSize(new java.awt.Dimension(480, 20));
        memoryJProgressBar.setPreferredSize(new java.awt.Dimension(480, 20));
        memoryJProgressBar.setRequestFocusEnabled(false);
        memoryJProgressBar.setString("Memory Test");
        memoryJProgressBar.setStringPainted(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        memoryJPanel.add(memoryJProgressBar, gridBagConstraints);

        memoryLabelJLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/install/MemoryLabel.png")));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        memoryJPanel.add(memoryLabelJLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(20, 15, 0, 15);
        contentJPanel.add(memoryJPanel, gridBagConstraints);

        cpuJPanel.setLayout(new java.awt.GridBagLayout());

        cpuJPanel.setOpaque(false);
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

        cpuJProgressBar.setFont(new java.awt.Font("Dialog", 0, 12));
        cpuJProgressBar.setForeground(new java.awt.Color(68, 91, 255));
        cpuJProgressBar.setValue(25);
        cpuJProgressBar.setFocusable(false);
        cpuJProgressBar.setMaximumSize(new java.awt.Dimension(480, 20));
        cpuJProgressBar.setMinimumSize(new java.awt.Dimension(480, 20));
        cpuJProgressBar.setPreferredSize(new java.awt.Dimension(480, 20));
        cpuJProgressBar.setRequestFocusEnabled(false);
        cpuJProgressBar.setString("CPU Test");
        cpuJProgressBar.setStringPainted(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        cpuJPanel.add(cpuJProgressBar, gridBagConstraints);

        cpuLabelJLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/install/CpuLabel.png")));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        cpuJPanel.add(cpuLabelJLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(20, 15, 0, 15);
        contentJPanel.add(cpuJPanel, gridBagConstraints);

        diskJPanel.setLayout(new java.awt.GridBagLayout());

        diskJPanel.setOpaque(false);
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

        diskJProgressBar.setFont(new java.awt.Font("Dialog", 0, 12));
        diskJProgressBar.setForeground(new java.awt.Color(68, 91, 255));
        diskJProgressBar.setValue(25);
        diskJProgressBar.setFocusable(false);
        diskJProgressBar.setMaximumSize(new java.awt.Dimension(480, 20));
        diskJProgressBar.setMinimumSize(new java.awt.Dimension(480, 20));
        diskJProgressBar.setPreferredSize(new java.awt.Dimension(480, 20));
        diskJProgressBar.setRequestFocusEnabled(false);
        diskJProgressBar.setString("Hard Disk Test");
        diskJProgressBar.setStringPainted(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        diskJPanel.add(diskJProgressBar, gridBagConstraints);

        diskLabelJLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/install/DiskLabel.png")));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        diskJPanel.add(diskLabelJLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(20, 15, 0, 15);
        contentJPanel.add(diskJPanel, gridBagConstraints);

        nicJPanel.setLayout(new java.awt.GridBagLayout());

        nicJPanel.setOpaque(false);
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

        nicJProgressBar.setFont(new java.awt.Font("Dialog", 0, 12));
        nicJProgressBar.setForeground(new java.awt.Color(68, 91, 255));
        nicJProgressBar.setValue(25);
        nicJProgressBar.setFocusable(false);
        nicJProgressBar.setMaximumSize(new java.awt.Dimension(480, 20));
        nicJProgressBar.setMinimumSize(new java.awt.Dimension(480, 20));
        nicJProgressBar.setPreferredSize(new java.awt.Dimension(480, 20));
        nicJProgressBar.setRequestFocusEnabled(false);
        nicJProgressBar.setString("Network Interface Test");
        nicJProgressBar.setStringPainted(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        nicJPanel.add(nicJProgressBar, gridBagConstraints);

        nicLabelJLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/install/NicsLabel.png")));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        nicJPanel.add(nicLabelJLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(20, 15, 0, 15);
        contentJPanel.add(nicJPanel, gridBagConstraints);

        resultJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        resultJLabel.setText(" ");
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

        backgroundJPabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/install/ProductShot.png")));
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
    private javax.swing.JLabel cpuLabelJLabel;
    private javax.swing.JLabel cpuNameJLabel;
    private javax.swing.JLabel cpuResultJLabel;
    private javax.swing.JPanel diskJPanel;
    private javax.swing.JProgressBar diskJProgressBar;
    private javax.swing.JLabel diskLabelJLabel;
    private javax.swing.JLabel diskNameJLabel;
    private javax.swing.JLabel diskResultJLabel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel memoryJPanel;
    private javax.swing.JProgressBar memoryJProgressBar;
    private javax.swing.JLabel memoryLabelJLabel;
    private javax.swing.JLabel memoryNameJLabel;
    private javax.swing.JLabel memoryResultJLabel;
    private javax.swing.JPanel nicJPanel;
    private javax.swing.JProgressBar nicJProgressBar;
    private javax.swing.JLabel nicLabelJLabel;
    private javax.swing.JLabel nicNameJLabel;
    private javax.swing.JLabel nicResultJLabel;
    private javax.swing.JLabel resultJLabel;
    // End of variables declaration//GEN-END:variables

}
