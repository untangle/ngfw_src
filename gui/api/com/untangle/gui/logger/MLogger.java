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

package com.untangle.gui.logger;

import java.io.*;
import javax.swing.*;
import java.awt.*;
import java.util.Date;
import java.util.Hashtable;
import java.util.Arrays;

public class MLogger extends javax.swing.JFrame implements FilenameFilter {

    // FILTERS /////////////
    private volatile boolean SHOW_APPLIANCE_LOGS = true;
    private volatile boolean SHOW_SYSTEM_LOGS = true;
    private volatile boolean SHOW_CURRENT_LOGS = true;
    private volatile boolean SHOW_BACKUP_LOGS = false;
    ////////////////////////

    // ACTIONS /////////////
    private volatile boolean DO_PAUSE = false;
    private volatile boolean DO_AUTOSCROLL = true;
    ////////////////////////

    private static final long THREAD_SLEEP_MILLIS = 3000l;
    
    private JTextArea[] logJTextAreas;
    private JScrollPane[] logJScrollPanes;
    private String[] logs;
    private BufferedInputStream[] logStreams;
    private boolean[] updateNeeded;
    private File[] logFiles;
    private File logDirectory;

    private int lastBufferSize = -1; // in lines
    private int currentBufferSize = 0;
    
    private StringBuffer outputStringBuffer;
    private JTextArea outputJTextArea;
    private JScrollPane outputJScrollPane;
    private int outputLastCount = 0;

    private volatile boolean deleteLogs = false;
    
    public MLogger(String[] args) {

        // COMMAND LINE ARGS
        if( (args == null) || (args.length != 1) ){
            System.err.println("Error: you must give the path to the log directory.  (Absolute or relative to the working directory)");
            return;
        }

	// GENERAL INIT
        logDirectory = new File(args[0]);
	outputStringBuffer = new StringBuffer();

        // GUI INIT
	SwingUtilities.invokeLater( new Runnable(){ public void run(){
	    initComponents();
	    // FILTER CHECKBOXES
	    showApplianceLogsJCheckBox.setSelected(SHOW_APPLIANCE_LOGS);
	    showSystemLogsJCheckBox.setSelected(SHOW_SYSTEM_LOGS);
	    showCurrentLogsJCheckBox.setSelected(SHOW_CURRENT_LOGS);
	    showBackupLogsJCheckBox.setSelected(SHOW_BACKUP_LOGS);
	    // ACTIONS
	    pauseJCheckBox.setSelected(DO_PAUSE);
	    autoscrollJCheckBox.setSelected(DO_AUTOSCROLL);
	    // OUTPUT PANEL
	    outputJTextArea = new JTextArea();
	    outputJScrollPane = new JScrollPane(outputJTextArea);
	    outputJTabbedPane.addTab("output (0)", outputJScrollPane);
	    // OUTPUT THE LOG DIRECTORY
	    try{
		outputStringBuffer.append("log directory: " + logDirectory.getCanonicalPath() + "\n");
		System.err.println("log directory: " + logDirectory.getCanonicalPath() );
	    }
	    catch(Exception e){ outputStringBuffer.append(e.toString()); }
	    outputJTextArea.append(outputStringBuffer.toString());
            setVisible(true);
        }});

        scanLogDirectory();
        
    }
    
    // says which files to accept as valid log files
    public boolean accept(java.io.File file, String str) {
	if( str == null )
	    return false;
	boolean isValidType = false;
	boolean isValidTime = false;
	if( SHOW_APPLIANCE_LOGS ){
	    int firstPeriod = str.indexOf('.');
	    try{
		String name = str.substring(0,firstPeriod);
		Integer.parseInt(name);
		isValidType |= true;
	    }
	    catch(Exception e){}
	}
	if( SHOW_SYSTEM_LOGS ){
	    int firstPeriod = str.indexOf('.');
	    try{
		if(firstPeriod >= 0){
		    String name = str.substring(0,firstPeriod);
		    Integer.parseInt(name);
		}
	    }
	    catch(Exception e){ isValidType |= true; }
	}
	if( SHOW_CURRENT_LOGS ){
	    if( str.endsWith(".log") )
		isValidTime |= true;
	}
	if( SHOW_BACKUP_LOGS ){
	    if( str.contains(".log.") )
		isValidTime |= true;
	}
	return isValidType && isValidTime;
    }    
        
        
    public void scanLogDirectory() {
        
        File[] newFiles;
            
	while(true){		
	    try{
                // PAUSE IF NECESSARY
                if(DO_PAUSE)
		    continue;
                
		// GET THE LIST OF FILES IN THE DIRECTORY
		newFiles = logDirectory.listFiles(this);
		
		// CLEAR OUTPUT BUFFER
		outputStringBuffer = new StringBuffer();

		// DETERMINE BUFFER SIZE
		SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
		    currentBufferSize = bufferJSlider.getValue();
		}});
		
		// CASE: NO FILES TO VIEW
		if( (newFiles == null) || (newFiles.length <= 0) ){
		    outputStringBuffer.append( (new Date(System.currentTimeMillis())).toString()
					       + " Error: no log files" + "\n");
		    initializeTabs();
		    logFiles = null;
		}		
		// CASE: FILE SET HAS CHANGED, OR BUFFER SIZE HAS CHANGED, DO NEW VISUALIZATIONS
		else if( !isSameFileSet(logFiles, newFiles) || (currentBufferSize != lastBufferSize) ){
		    lastBufferSize = currentBufferSize;
		    outputStringBuffer.append( (new Date(System.currentTimeMillis())).toString()
					       + " log files set updated...  rereading log files." + "\n");		    
		    for( File newFile : newFiles )
			outputStringBuffer.append( (new Date(System.currentTimeMillis())).toString()
						   + " Found new file: " 
						   + newFile.getCanonicalPath() + "\n");
		    // CLOSE OLD STREAMS
		    if( logStreams != null )
			for( BufferedInputStream logStream : logStreams ){
			    try{ logStream.close(); }
			    catch(Exception e){ outputStringBuffer.append( (new Date(System.currentTimeMillis())).toString()
									   + " Couldnt close old file: "
									   + logStream.toString()
									   + "\n");
			    }
			}
		    
		    // CREATE AND INITIALIZE THE PROPER AMOUNT OF GENERAL RESOURCES
		    logFiles = newFiles;
		    logStreams = new BufferedInputStream[logFiles.length];
		    updateNeeded = new boolean[logFiles.length];
		    logs = new String[logFiles.length];
		    for(int i = 0; i < logFiles.length; i++){
			logStreams[i] = new BufferedInputStream( new FileInputStream(logFiles[i]) );
			updateNeeded[i] = true;
		    }
		    // CREATE AND INITIALIZE THE PROPER AMOUNT OF GUI RESOURECES
		    SwingUtilities.invokeLater( new Runnable(){ public void run(){
			logJTextAreas = new JTextArea[logFiles.length];
			logJScrollPanes = new JScrollPane[logFiles.length];
			outputJTabbedPane.removeAll();
			outputJTabbedPane.addTab("output", outputJScrollPane);
			for(int i = 0; i < logFiles.length; i++){
			    logJTextAreas[i] = new JTextArea();
			    logJTextAreas[i].setEditable(false);
			    Insets margin = logJTextAreas[i].getMargin();
			    margin.left = 10;
			    margin.right = 10;
			    logJTextAreas[i].setMargin(margin);
			    logJScrollPanes[i] = new JScrollPane(logJTextAreas[i]);
			    outputJTabbedPane.addTab(logFiles[i].getName(), logJScrollPanes[i]);
			}
		    }});
		    // SHOW UPDATES
		    updateLogs();
		}
		// CASE: SAME FILE SET, SIMPLY UPDATE
		else{
		    updateLogs();
		}
		
		// UPDATE OUTPUT BUFFER
		SwingUtilities.invokeLater( new Runnable(){ public void run(){
		    outputJTextArea.append(outputStringBuffer.toString());
		    outputLastCount = outputStringBuffer.length();		
		    outputJTabbedPane.setTitleAt(0, "output");				
		}});
	    } 
	    catch(Exception e){ e.printStackTrace(); }
	    
            // SLEEP
            try{ Thread.sleep(THREAD_SLEEP_MILLIS); }
            catch(Exception e){ e.printStackTrace(); }
	    
	}
    }
    
    
    
    
    private void initializeTabs(){
	SwingUtilities.invokeLater( new Runnable(){ public void run(){
	    int tabCount = outputJTabbedPane.getTabCount();
	    if( tabCount > 1){
		while(outputJTabbedPane.getTabCount() > 1){
		    outputJTabbedPane.remove(outputJTabbedPane.getTabCount()-1);
		}
		outputJTabbedPane.setTitleAt(0, "output");
	    }
	    else if(tabCount == 1){
		outputJTabbedPane.setTitleAt(0, "output");
	    }
	    else{
		outputJTabbedPane.addTab("output", outputJScrollPane);
	    }
	    outputJTextArea.setCaretPosition(outputJTextArea.getText().length());
	}});
    }
    
    
    private void updateLogs(){
	// READ FROM ALL STREAMS
	for(int i = 0; i < logStreams.length; i++){
	    try{
                if( logStreams[i].available() <= 0 ){
		    updateNeeded[i] = false;
                    continue;
		}
		else
		    updateNeeded[i] = true;
		// APPEND TO OUTPUT BUFFER
                outputStringBuffer.append( new Date()
					   + " Updating: "
					   + logFiles[i].getCanonicalPath() + "\n");
		// READ A STREAM
                byte[] buffer = new byte[logStreams[i].available()];
                logStreams[i].read(buffer);
                logs[i] = new String( buffer );
	    }
	    catch(Exception e){ e.printStackTrace(); }
	}
	// UPDATE ONLY CHANGED STREAMS AND OUTPUT BUFFER
	try{
	    SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
		int maxLineCount = bufferJSlider.getValue();
		int currentLineCount;
		// DO EACH OF THE LOGS
		for(int i = 0; i < logStreams.length; i++){
		    if( updateNeeded[i] ){
			logJTextAreas[i].append( logs[i] );
			outputJTabbedPane.setTitleAt(1 + i, "<html><font color=\"#FF0000\">"
						     + logFiles[i].getName() + "</font></html>");
			currentLineCount = logJTextAreas[i].getLineCount();
			if( currentLineCount > maxLineCount ){
			    try{
				int lineEndOffset = logJTextAreas[i].getLineEndOffset(currentLineCount-maxLineCount);
				logJTextAreas[i].getDocument().remove(0,lineEndOffset);
			    } catch(Exception e){ e.printStackTrace(); }
			}
			if(DO_AUTOSCROLL){
			    JScrollBar scrollBar = logJScrollPanes[i].getVerticalScrollBar();
			    scrollBar.setValue(scrollBar.getMaximum());
			}
		    }
		}
		// DO OUTPUT PANEL
		currentLineCount = outputJTextArea.getLineCount();
		if( currentLineCount > maxLineCount ){
		    try{
			int lineEndOffset = outputJTextArea.getLineEndOffset(currentLineCount-maxLineCount);
			outputJTextArea.getDocument().remove(0,lineEndOffset);
		    } catch(Exception e){ e.printStackTrace(); }
		}		
		if(DO_AUTOSCROLL){
		    JScrollBar scrollBar = outputJScrollPane.getVerticalScrollBar();
		    scrollBar.setValue(scrollBar.getMaximum());
		}
	    }});
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

	Hashtable<File,Object> fileHashtable = new Hashtable<File,Object>();
        for( File file : fileSetA )
	    fileHashtable.put( file, file );
	for( File file : fileSetB )
	    if( !fileHashtable.containsKey(file) )
		return false;

        return true;
    }


    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        outputJTabbedPane = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        pauseJCheckBox = new javax.swing.JCheckBox();
        autoscrollJCheckBox = new javax.swing.JCheckBox();
        jPanel2 = new javax.swing.JPanel();
        showApplianceLogsJCheckBox = new javax.swing.JCheckBox();
        showSystemLogsJCheckBox = new javax.swing.JCheckBox();
        jPanel3 = new javax.swing.JPanel();
        showCurrentLogsJCheckBox = new javax.swing.JCheckBox();
        showBackupLogsJCheckBox = new javax.swing.JCheckBox();
        jPanel4 = new javax.swing.JPanel();
        bufferJSlider = new javax.swing.JSlider();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Untangle Logger by Ian");
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
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(outputJTabbedPane, gridBagConstraints);

        jPanel1.setLayout(new javax.swing.BoxLayout(jPanel1, javax.swing.BoxLayout.Y_AXIS));

        jPanel1.setBorder(new javax.swing.border.TitledBorder(null, "Actions", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 11)));
        pauseJCheckBox.setFont(new java.awt.Font("Dialog", 0, 12));
        pauseJCheckBox.setText("pause");
        pauseJCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pauseJCheckBoxActionPerformed(evt);
            }
        });

        jPanel1.add(pauseJCheckBox);

        autoscrollJCheckBox.setFont(new java.awt.Font("Dialog", 0, 12));
        autoscrollJCheckBox.setText("autoscroll");
        autoscrollJCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                autoscrollJCheckBoxActionPerformed(evt);
            }
        });

        jPanel1.add(autoscrollJCheckBox);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(jPanel1, gridBagConstraints);

        jPanel2.setLayout(new javax.swing.BoxLayout(jPanel2, javax.swing.BoxLayout.Y_AXIS));

        jPanel2.setBorder(new javax.swing.border.TitledBorder(null, "Filter: Type", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 11)));
        showApplianceLogsJCheckBox.setFont(new java.awt.Font("Dialog", 0, 12));
        showApplianceLogsJCheckBox.setText("appliances");
        showApplianceLogsJCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showApplianceLogsJCheckBoxActionPerformed(evt);
            }
        });

        jPanel2.add(showApplianceLogsJCheckBox);

        showSystemLogsJCheckBox.setFont(new java.awt.Font("Dialog", 0, 12));
        showSystemLogsJCheckBox.setText("system");
        showSystemLogsJCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showSystemLogsJCheckBoxActionPerformed(evt);
            }
        });

        jPanel2.add(showSystemLogsJCheckBox);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 5);
        getContentPane().add(jPanel2, gridBagConstraints);

        jPanel3.setLayout(new javax.swing.BoxLayout(jPanel3, javax.swing.BoxLayout.Y_AXIS));

        jPanel3.setBorder(new javax.swing.border.TitledBorder(null, "Filter: Time", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 11)));
        showCurrentLogsJCheckBox.setFont(new java.awt.Font("Dialog", 0, 12));
        showCurrentLogsJCheckBox.setText("current");
        showCurrentLogsJCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showCurrentLogsJCheckBoxActionPerformed(evt);
            }
        });

        jPanel3.add(showCurrentLogsJCheckBox);

        showBackupLogsJCheckBox.setFont(new java.awt.Font("Dialog", 0, 12));
        showBackupLogsJCheckBox.setText("backup");
        showBackupLogsJCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showBackupLogsJCheckBoxActionPerformed(evt);
            }
        });

        jPanel3.add(showBackupLogsJCheckBox);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.ipadx = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 5);
        getContentPane().add(jPanel3, gridBagConstraints);

        jPanel4.setLayout(new java.awt.GridBagLayout());

        jPanel4.setBorder(new javax.swing.border.TitledBorder(null, "Tail Length (lines)", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 11)));
        bufferJSlider.setFont(new java.awt.Font("Dialog", 0, 12));
        bufferJSlider.setMajorTickSpacing(1000);
        bufferJSlider.setMaximum(5000);
        bufferJSlider.setMinimum(10);
        bufferJSlider.setPaintLabels(true);
        bufferJSlider.setPaintTicks(true);
        bufferJSlider.setValue(200);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 10);
        jPanel4.add(bufferJSlider, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 5);
        getContentPane().add(jPanel4, gridBagConstraints);

        java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        setBounds((screenSize.width-640)/2, (screenSize.height-480)/2, 640, 480);
    }//GEN-END:initComponents

    private void autoscrollJCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_autoscrollJCheckBoxActionPerformed
        DO_AUTOSCROLL = autoscrollJCheckBox.isSelected();
    }//GEN-LAST:event_autoscrollJCheckBoxActionPerformed

    private void pauseJCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pauseJCheckBoxActionPerformed
        DO_PAUSE = pauseJCheckBox.isSelected();
    }//GEN-LAST:event_pauseJCheckBoxActionPerformed

    private void showBackupLogsJCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showBackupLogsJCheckBoxActionPerformed
        SHOW_BACKUP_LOGS = showBackupLogsJCheckBox.isSelected();
    }//GEN-LAST:event_showBackupLogsJCheckBoxActionPerformed

    private void showCurrentLogsJCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showCurrentLogsJCheckBoxActionPerformed
        SHOW_CURRENT_LOGS = showCurrentLogsJCheckBox.isSelected();
    }//GEN-LAST:event_showCurrentLogsJCheckBoxActionPerformed

    private void showApplianceLogsJCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showApplianceLogsJCheckBoxActionPerformed
        SHOW_APPLIANCE_LOGS = showApplianceLogsJCheckBox.isSelected();
    }//GEN-LAST:event_showApplianceLogsJCheckBoxActionPerformed

    private void showSystemLogsJCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showSystemLogsJCheckBoxActionPerformed
        SHOW_SYSTEM_LOGS = showSystemLogsJCheckBox.isSelected();
    }//GEN-LAST:event_showSystemLogsJCheckBoxActionPerformed

    private void outputJTabbedPaneStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_outputJTabbedPaneStateChanged
        int index = outputJTabbedPane.getSelectedIndex();
        
        if(index == 0)
            outputJTabbedPane.setTitleAt(0, "output");
        else if(index > 0){
            outputJTabbedPane.setTitleAt(index, logFiles[index-1].getName() );
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
	    /*
          com.incors.plaf.kunststoff.KunststoffLookAndFeel kunststoffLnF = new com.incors.plaf.kunststoff.KunststoffLookAndFeel();
          kunststoffLnF.setCurrentTheme(new com.incors.plaf.kunststoff.KunststoffTheme());
          UIManager.setLookAndFeel(kunststoffLnF);
          System.out.println(UIManager.getLookAndFeel().getDescription());
	    */
        }
        catch (Exception e) {
           // handle exception or not, whatever you prefer
            e.printStackTrace();
        }
                
        new MLogger(args);
    }
    


    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox autoscrollJCheckBox;
    private javax.swing.JSlider bufferJSlider;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JTabbedPane outputJTabbedPane;
    private javax.swing.JCheckBox pauseJCheckBox;
    private javax.swing.JCheckBox showApplianceLogsJCheckBox;
    private javax.swing.JCheckBox showBackupLogsJCheckBox;
    private javax.swing.JCheckBox showCurrentLogsJCheckBox;
    private javax.swing.JCheckBox showSystemLogsJCheckBox;
    // End of variables declaration//GEN-END:variables
    
}
