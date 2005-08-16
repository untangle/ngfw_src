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

package com.metavize.gui.logger;

import java.io.*;
import javax.swing.*;
import java.awt.*;


public class MLogger extends javax.swing.JFrame implements Runnable, FilenameFilter {
    
    private static final long THREAD_SLEEP_MILLIS = 1500l;
    
    private JTextArea[] logJTextAreas;
    private JScrollPane[] logJScrollPanes;
    private StringBuffer[] logs;
    private BufferedInputStream[] logStreams;
    private File[] logFiles;
    private File logDirectory;
    private int[] logLastCount;

    
    private StringBuffer outputStringBuffer;
    private JTextArea outputJTextArea;
    private JScrollPane outputJScrollPane;
    private int outputLastCount = 0;

    private volatile boolean deleteLogs = false;
    
    public MLogger(String[] args) {
        
        if( (args == null) || (args.length != 1) ){
            System.err.println("Error: you should give the relative path from this executable to the log directory");
            return;
        }
        
        initComponents();
        outputStringBuffer = new StringBuffer();
        outputJTextArea = new JTextArea();
        outputJScrollPane = new JScrollPane(outputJTextArea);
        outputJTabbedPane.addTab("output (0)", outputJScrollPane);
        
        logDirectory = new File(args[0]);
        try{
            outputStringBuffer.append("log directory: " + logDirectory.getCanonicalPath() + "\n");
            outputJTextArea.setText(outputStringBuffer.toString());
        }
        catch(Exception e){System.err.println(e);}

        
        (new Thread(this)).start();
        
    }
    
    // says which files to accept as valid log files
    public boolean accept(java.io.File file, String str) {
        if( (str != null) && (str.endsWith(".log")) )
            return true;
        else
            return false;
    }    
        
        
    public void run() {
        
        File[] newFiles;
        
        
        try{
        while(true){
        
            // wait a bit before reading log files
            Thread.sleep(THREAD_SLEEP_MILLIS);
            if(pauseJCheckBox.isSelected())
                continue;

            // get a list of all the log files
            newFiles = logDirectory.listFiles(this);
            
            // deal with 2 cases where there are no files to view
            if( newFiles == null ){
                outputStringBuffer.append( System.currentTimeMillis() + " Error: null log files" + "\n");
                outputJTextArea.setText(outputStringBuffer.toString());
                initializeTabs();
                logFiles = null;
                deleteLogs = false;
            }
            else if( newFiles.length <= 0 ){
                outputStringBuffer.append( System.currentTimeMillis() + " Error: no log files" + "\n");
                outputJTextArea.setText(outputStringBuffer.toString());
                initializeTabs();
                logFiles = null;
                deleteLogs = false;
            }
            
            // if the set of log files has changed, create a new set of log visualizations
            else if( !isSameFileSet(logFiles, newFiles) ){
                outputStringBuffer.append( System.currentTimeMillis() + " log files set updated...  rereading log files." + "\n");
                
                for(int i=0; i<newFiles.length; i++){
                    outputStringBuffer.append( System.currentTimeMillis() + " Found new file: " + newFiles[i].getCanonicalPath() + "\n");
                }
                
                
                if(logStreams != null){
                    for(int i=0; i< logStreams.length; i++)
                        try{logStreams[i].close();}
                        catch(Exception e){
                            outputStringBuffer.append( System.currentTimeMillis() + " Couldnt close old file. no prob." + "\n");
                        }
                }
                logFiles = newFiles;
                logStreams = new BufferedInputStream[logFiles.length];
                logs = new StringBuffer[logFiles.length];
                logJTextAreas = new JTextArea[logFiles.length];
                logJScrollPanes = new JScrollPane[logFiles.length];
                logLastCount = new int[logFiles.length];
                outputJTabbedPane.removeAll();
                outputJTabbedPane.addTab("output (0)", outputJScrollPane);
                for(int i = 0; i < logFiles.length; i++){
                    logStreams[i] = new BufferedInputStream( new FileInputStream(logFiles[i]) );
                    logs[i] = new StringBuffer();
                    logJTextAreas[i] = new JTextArea();
                    logJScrollPanes[i] = new JScrollPane(logJTextAreas[i]);
                    outputJTabbedPane.addTab(logFiles[i].getName() + " (0)", logJScrollPanes[i] );
                }
                updateLogs();
                deleteLogs = false;
            }
            else{
                updateLogs();
                deleteLogs = false;
            }

            // update buffers and counts
            outputJTextArea.setText(outputStringBuffer.toString());
            outputLastCount = outputStringBuffer.length();
            
            outputJTabbedPane.setTitleAt(0, "output (" + outputLastCount + ")");

            
        } 
        }
        catch(Exception e){ e.printStackTrace(); }
    }

   
    private void initializeTabs(){
        int tabCount = outputJTabbedPane.getTabCount();
        if( tabCount > 1){
            while(outputJTabbedPane.getTabCount() > 1){
                outputJTabbedPane.remove(outputJTabbedPane.getTabCount()-1);
            }
            outputJTabbedPane.setTitleAt(0, "output (" + outputLastCount + ")");
        }
        else if(tabCount == 1){
            outputJTabbedPane.setTitleAt(0, "output (" + outputLastCount + ")");
        }
        else{
            outputJTabbedPane.addTab("output(0)", outputJScrollPane);
        }
        outputJTextArea.setCaretPosition(outputJTextArea.getText().length());
    }
    
    
    private void updateLogs(){
        try{
        if(!deleteLogs){    
            for(int i = 0; i < logStreams.length; i++){
                if( logStreams[i].available() <= 0 )
                    continue;
                outputStringBuffer.append( System.currentTimeMillis() + " Updating: " + logFiles[i].getCanonicalPath() + "\n");
                outputJTextArea.setText(outputStringBuffer.toString());
                byte[] buffer = new byte[ logStreams[i].available()];
                logStreams[i].read(buffer);
                logs[i].append( new String( buffer ) );
                logJTextAreas[i].setText( logs[i].toString() );
                logLastCount[i] = logs[i].length();
                outputJTabbedPane.setTitleAt(1 + i, "**" + logFiles[i].getName() + " (" + logLastCount[i] + ")**");
                if(autoscrollJCheckBox.isSelected()){
                    logJTextAreas[i].setCaretPosition(logJTextAreas[i].getText().length());
                    outputJTextArea.setCaretPosition(outputJTextArea.getText().length());
                }
            }
        }
        else{
            for(int i = 0; i < logFiles.length; i++){
                outputStringBuffer.append("deleding log file: " + logFiles[i].getCanonicalPath() + " result: " + logFiles[i].delete() + "\n");
                logStreams[i].close();
            }
        }
        }
        catch(Exception e){ e.printStackTrace(); }
        
        
    }
    
    private boolean isSameFileSet(File[] fileSetA, File[] fileSetB){
        if( fileSetA == fileSetB )
            return true;
        else if( (fileSetA == null) && (fileSetB != null) && (fileSetB.length > 0) )
            return false;
        else if( (fileSetA != null) && (fileSetB == null) && (fileSetA.length > 0) )
            return false;
        else if( (fileSetA == null) && (fileSetB != null) && (fileSetB.length == 0) )
            return true;
        else if( (fileSetA != null) && (fileSetB == null) && (fileSetA.length == 0) )
            return true;
        
        if(fileSetA.length != fileSetB.length)
            return false;
        
        boolean foundFile;
        
        for(int i=0; i<fileSetA.length; i++){
            foundFile = false;
            for(int j=0; j<fileSetB.length; j++){
                if( fileSetA[i].equals(fileSetB[j]) )
                    foundFile = true;
            }
            if(foundFile == true)
                continue;
            else
                return false;
        }
            
        
        return true;
    }
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        outputJTabbedPane = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        pauseJCheckBox = new javax.swing.JCheckBox();
        autoscrollJCheckBox = new javax.swing.JCheckBox();
        deleteLogsJButton = new javax.swing.JButton();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Metavize Logger");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                exitForm(evt);
            }
        });

        outputJTabbedPane.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                outputJTabbedPaneStateChanged(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(outputJTabbedPane, gridBagConstraints);

        pauseJCheckBox.setText("pause");
        jPanel1.add(pauseJCheckBox);

        autoscrollJCheckBox.setText("autoscroll");
        autoscrollJCheckBox.setSelected(true);
        jPanel1.add(autoscrollJCheckBox);

        deleteLogsJButton.setFont(new java.awt.Font("Dialog", 0, 12));
        deleteLogsJButton.setText("Delete All Logs");
        deleteLogsJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteLogsJButtonActionPerformed(evt);
            }
        });

        jPanel1.add(deleteLogsJButton);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(jPanel1, gridBagConstraints);

        java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        setBounds((screenSize.width-640)/2, (screenSize.height-480)/2, 640, 480);
    }//GEN-END:initComponents

    private void deleteLogsJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteLogsJButtonActionPerformed
        deleteLogs = true;
    }//GEN-LAST:event_deleteLogsJButtonActionPerformed

    private void outputJTabbedPaneStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_outputJTabbedPaneStateChanged
        int index = outputJTabbedPane.getSelectedIndex();
        
        if(index == 0)
            outputJTabbedPane.setTitleAt(0, "output (" + outputLastCount + ")");
        else if(index > 0){
            outputJTabbedPane.setTitleAt(index, logFiles[index-1].getName() + " (" + logLastCount[index-1] + ")");
        }
    }//GEN-LAST:event_outputJTabbedPaneStateChanged
    
    /** Exit the Application */
    private void exitForm(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_exitForm
        System.exit(0);
    }//GEN-LAST:event_exitForm
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        
        try {
          com.incors.plaf.kunststoff.KunststoffLookAndFeel kunststoffLnF = new com.incors.plaf.kunststoff.KunststoffLookAndFeel();
          kunststoffLnF.setCurrentTheme(new com.incors.plaf.kunststoff.KunststoffTheme());
          UIManager.setLookAndFeel(kunststoffLnF);
          System.out.println(UIManager.getLookAndFeel().getDescription());
        }
        catch (Exception e) {
           // handle exception or not, whatever you prefer
            e.printStackTrace();
        }
                
        new MLogger(args).setVisible(true);
    }
    


    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox autoscrollJCheckBox;
    private javax.swing.JButton deleteLogsJButton;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JTabbedPane outputJTabbedPane;
    private javax.swing.JCheckBox pauseJCheckBox;
    // End of variables declaration//GEN-END:variables
    
}
