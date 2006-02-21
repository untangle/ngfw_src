/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: MConfigJDialog.java 194 2005-04-06 19:13:55Z inieves $
 */

package com.metavize.gui.widgets.dialogs;

import com.metavize.gui.transform.*;
import com.metavize.gui.widgets.coloredTable.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;

import com.metavize.mvvm.security.PasswordUtil;
import com.metavize.mvvm.*;
import com.metavize.mvvm.tran.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.text.*;
import java.util.*;

public abstract class MConfigJDialog extends javax.swing.JDialog implements java.awt.event.WindowListener {
    
    // SAVING/REFRESHING ///////////
    private Map<String, Refreshable> refreshableMap = new LinkedHashMap(5);
    protected void addRefreshable(String name, Refreshable refreshable){ refreshableMap.put(name, refreshable); }
    private Map<String, Savable> savableMap = new LinkedHashMap(5);
    protected void addSavable(String name, Savable savable){ savableMap.put(name, savable); }
    private Map<String, Shutdownable> shutdownableMap = new LinkedHashMap(1);
    protected void addShutdownable(String name, Shutdownable shutdownable){ shutdownableMap.put(name, shutdownable); }
    protected CompoundSettings compoundSettings;
    private static InfiniteProgressJComponent infiniteProgressJComponent = new InfiniteProgressJComponent();
    public static InfiniteProgressJComponent getInfiniteProgressJComponent(){ return infiniteProgressJComponent; }
    private static final long MIN_PROGRESS_MILLIS = 1000;
    ///////////////////////////////

    public MConfigJDialog(Dialog parentDialog){
	super(parentDialog, true);
	init(parentDialog);
    }

    public MConfigJDialog() {
	super(Util.getMMainJFrame(), true);
	init(Util.getMMainJFrame());
    }
    
    private void init(Window parentWindow){
	getRootPane().setDoubleBuffered(true);
	RepaintManager.currentManager(this).setDoubleBufferingEnabled(true);
        this.initComponents();
        this.setBounds( Util.generateCenteredBounds( parentWindow.getBounds(), getMinSize().width, getMinSize().height) );
        this.addWindowListener(this);
	this.setGlassPane(infiniteProgressJComponent);
        this.addComponentListener( new ComponentAdapter() { public void componentResized(ComponentEvent evt) {
	    dialogResized();
	}});
    }

    public void setVisible(boolean isVisible){
	if(isVisible){
	    new RefreshAllThread(true);
	}
	super.setVisible(isVisible);
    }

    protected abstract void generateGui();

    // TABS AND TABBED PANES //////////////
    public void addTab(String name, Icon icon, Component component){ contentJTabbedPane.addTab(name, icon, component); }
    public JTabbedPane addTabbedPane(String name, Icon icon){
        JTabbedPane newJTabbedPane = new JTabbedPane();
        newJTabbedPane.setBorder(new EmptyBorder(7, 13, 13, 13));
        newJTabbedPane.setFocusable(false);
        newJTabbedPane.setFont(new java.awt.Font("Arial", 0, 11));
	newJTabbedPane.setRequestFocusEnabled(false);
	addTab(name, icon, newJTabbedPane);
	return newJTabbedPane;
    }
    public JScrollPane addScrollableTab(JTabbedPane parentJTabbedPane, String name, Icon icon,
					Component childComponent, boolean scrollH, boolean scrollV){
	JScrollPane newJScrollPane = new JScrollPane(childComponent);
	newJScrollPane.setHorizontalScrollBarPolicy( scrollH ? JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS : JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
	newJScrollPane.setVerticalScrollBarPolicy( scrollV ? JScrollPane.VERTICAL_SCROLLBAR_ALWAYS : JScrollPane.VERTICAL_SCROLLBAR_NEVER );
	if( parentJTabbedPane != null )
	    parentJTabbedPane.addTab(name, icon, newJScrollPane);
	else
	    addTab(name, icon, newJScrollPane);
	return newJScrollPane;
    }
    //////////////////////////////////////

    private Exception saveException;
    protected void saveAll() throws Exception {
	// GATHER ALL SETTINGS AND VALIDATE INDIVIDUALLY
	for( Map.Entry<String, Savable> savableMapEntry : savableMap.entrySet() ){
	    String componentName = savableMapEntry.getKey();
	    final Savable savableComponent = savableMapEntry.getValue();
	    saveException = null;
	    SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
		try{ savableComponent.doSave(compoundSettings, false); }
		catch(Exception e){
		    saveException = e;
		}
	    }});
	    if(saveException != null){
		ValidateFailureDialog.factory( (Window) MConfigJDialog.this,
					       getTitle(), componentName, saveException.getMessage() );
		throw new ValidationException();
	    }
	}
	// VALIDATE SIMULTANEOUSLY
	try{
	    compoundSettings.validate();
	}
	catch(Exception e){
	    ValidateFailureDialog.factory( (Window) MConfigJDialog.this,
					   getTitle(), "multiple settings panels", e.getMessage() );
	    throw new ValidationException();
	}
	// SEND SETTINGS TO SERVER
	compoundSettings.save();
    }


    protected void refreshAll() throws Exception {
	// GET SETTINGS FROM SERVER
	compoundSettings.refresh();
    }

    protected void populateAll() throws Exception {
	// UPDATE PANELS WITH NEW SETTINGS
	for( Map.Entry<String, Refreshable> refreshableMapEntry : refreshableMap.entrySet() ){
	    final String componentName = refreshableMapEntry.getKey();
	    final Refreshable refreshableComponent = refreshableMapEntry.getValue();	    
	    SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
		try{ refreshableComponent.doRefresh(compoundSettings); }
		catch(Exception e){
		    Util.handleExceptionNoRestart("Error distributing settings", e);
		    RefreshFailureDialog.factory( (Window) MConfigJDialog.this,
						  componentName );
		}
	    }});
	}
    }
    ////////////////////////////////////////////

    // SIZING ///////////////////////////////
    protected Dimension getMinSize(){
	return new Dimension(640, 480);
    }
    protected Dimension getMaxSize(){
	return new Dimension(1600, 1200);
    }
    private void dialogResized(){
        Util.resizeCheck(this, getMinSize(), getMaxSize());
    }
    ////////////////////////////////////////

    // ACTION BUTTONS /////////////////////
    public void refreshGui(){
	SwingUtilities.invokeLater( new Runnable(){ public void run(){
	    reloadJButton.doClick();
	}});
    }
    public void removeActionButtons(){
	reloadJButton.setVisible(false);
	saveJButton.setVisible(false);
    }
    ///////////////////////////////////////

    
        private void initComponents() {//GEN-BEGIN:initComponents
                java.awt.GridBagConstraints gridBagConstraints;

                buttonGroup1 = new javax.swing.ButtonGroup();
                contentJTabbedPane = new javax.swing.JTabbedPane();
                closeJButton = new javax.swing.JButton();
                reloadJButton = new javax.swing.JButton();
                saveJButton = new javax.swing.JButton();
                backgroundJLabel = new com.metavize.gui.widgets.MTiledIconLabel();

                getContentPane().setLayout(new java.awt.GridBagLayout());

                setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
                setTitle("");
                setModal(true);
                contentJTabbedPane.setDoubleBuffered(true);
                contentJTabbedPane.setFocusable(false);
                contentJTabbedPane.setFont(new java.awt.Font("Default", 0, 12));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.gridwidth = 3;
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(15, 15, 15, 15);
                getContentPane().add(contentJTabbedPane, gridBagConstraints);

                closeJButton.setFont(new java.awt.Font("Default", 0, 12));
                closeJButton.setText("<html><b>Close</b> Window</html>");
                closeJButton.setDoubleBuffered(true);
                closeJButton.setFocusPainted(false);
                closeJButton.setFocusable(false);
                closeJButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
                closeJButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
                closeJButton.setMaximumSize(new java.awt.Dimension(140, 25));
                closeJButton.setMinimumSize(new java.awt.Dimension(140, 25));
                closeJButton.setPreferredSize(new java.awt.Dimension(140, 25));
                closeJButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                closeJButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHWEST;
                gridBagConstraints.insets = new java.awt.Insets(0, 15, 15, 0);
                getContentPane().add(closeJButton, gridBagConstraints);

                reloadJButton.setFont(new java.awt.Font("Arial", 0, 12));
                reloadJButton.setText("<html><b>Refresh</b> Settings</html>");
                reloadJButton.setDoubleBuffered(true);
                reloadJButton.setFocusPainted(false);
                reloadJButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
                reloadJButton.setMargin(new java.awt.Insets(2, 2, 2, 2));
                reloadJButton.setMaximumSize(new java.awt.Dimension(120, 25));
                reloadJButton.setMinimumSize(new java.awt.Dimension(120, 25));
                reloadJButton.setPreferredSize(new java.awt.Dimension(120, 25));
                reloadJButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                reloadJButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHEAST;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 15, 15);
                getContentPane().add(reloadJButton, gridBagConstraints);

                saveJButton.setFont(new java.awt.Font("Arial", 0, 12));
                saveJButton.setText("<html><b>Save</b> Settings</html>");
                saveJButton.setDoubleBuffered(true);
                saveJButton.setFocusPainted(false);
                saveJButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
                saveJButton.setMargin(new java.awt.Insets(2, 2, 2, 2));
                saveJButton.setMaximumSize(new java.awt.Dimension(78, 25));
                saveJButton.setMinimumSize(new java.awt.Dimension(78, 25));
                saveJButton.setPreferredSize(new java.awt.Dimension(78, 25));
                saveJButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                saveJButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 2;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.ipadx = 40;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHEAST;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 15, 15);
                getContentPane().add(saveJButton, gridBagConstraints);

                backgroundJLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/metavize/gui/images/DarkGreyBackground1600x100.png")));
                backgroundJLabel.setDoubleBuffered(true);
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.gridwidth = 3;
                gridBagConstraints.gridheight = 2;
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                getContentPane().add(backgroundJLabel, gridBagConstraints);

        }//GEN-END:initComponents

    private void saveJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveJButtonActionPerformed
	if( Util.getIsDemo() )
	    return;
        new SaveAllThread();
    }//GEN-LAST:event_saveJButtonActionPerformed

    private void reloadJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reloadJButtonActionPerformed
        new RefreshAllThread(false);
    }//GEN-LAST:event_reloadJButtonActionPerformed

    private void closeJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeJButtonActionPerformed
        windowClosing(null);
    }//GEN-LAST:event_closeJButtonActionPerformed

    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
        this.setVisible(false);
        this.dispose();
    }

    public void windowClosed(java.awt.event.WindowEvent windowEvent) {}
    public void windowActivated(java.awt.event.WindowEvent windowEvent) {}
    public void windowDeactivated(java.awt.event.WindowEvent windowEvent) {}
    public void windowDeiconified(java.awt.event.WindowEvent windowEvent) {}
    public void windowIconified(java.awt.event.WindowEvent windowEvent) {}
    public void windowOpened(java.awt.event.WindowEvent windowEvent) {}


        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JLabel backgroundJLabel;
        private javax.swing.ButtonGroup buttonGroup1;
        private javax.swing.JButton closeJButton;
        protected javax.swing.JTabbedPane contentJTabbedPane;
        protected javax.swing.JButton reloadJButton;
        protected javax.swing.JButton saveJButton;
        // End of variables declaration//GEN-END:variables

    private class SaveAllThread extends Thread{
        public SaveAllThread(){
	    super("MVCLIENT-MConfigJDialog.SaveAllThread");
	    setDaemon(true);
	    infiniteProgressJComponent.start("Saving...");
            start();
        }        
        public void run(){
	    long startTime = System.currentTimeMillis();
	    try{
		MConfigJDialog.this.saveAll();
		MConfigJDialog.this.refreshAll();
		MConfigJDialog.this.populateAll();
	    }
	    catch(Exception e){
		try{ Util.handleExceptionWithRestart("Error sending saved settings", e); }
		catch(ValidationException f){ /* this was handled at a lower layer*/ }
		catch(Exception g){
		    Util.handleExceptionNoRestart("Error sending saved settings", g);
		    SaveFailureDialog.factory( (Window) MConfigJDialog.this,
					       MConfigJDialog.this.getTitle() );
		}
	    }
	    infiniteProgressJComponent.stopLater(MIN_PROGRESS_MILLIS);
        }
    }
        
    private Exception generateGuiException;
    private class RefreshAllThread extends Thread{
	private boolean doGenerateGui;
        public RefreshAllThread(boolean doGenerateGui){
	    super("MVCLIENT-MConfigJDialog.RefreshAllThread");
	    setDaemon(true);
	    this.doGenerateGui = doGenerateGui;
	    // START INFINITE PROGRESS
	    infiniteProgressJComponent.start("Refreshing...");
            start();
        }
        public void run(){
	    long startTime = System.currentTimeMillis();
	    // INIT COMPOUND SETTINGS
	    try{
		MConfigJDialog.this.refreshAll();
		if(doGenerateGui){
		    generateGuiException = null;
		    SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
			MConfigJDialog.this.generateGui();
		    }});
		    if( generateGuiException != null )
			throw generateGuiException;
		}
		MConfigJDialog.this.populateAll();
	    }
	    catch(Exception e){
		try{ Util.handleExceptionWithRestart("Error refreshing settings", e); }
		catch(Exception f){
		    Util.handleExceptionNoRestart("Error refreshing settings", f);
		    RefreshFailureDialog.factory( (Window) MConfigJDialog.this,
						  MConfigJDialog.this.getTitle() );
		}
	    }
	    // END INFINITE PROGRESS
	    infiniteProgressJComponent.stopLater(MIN_PROGRESS_MILLIS);
        }
    }
}







