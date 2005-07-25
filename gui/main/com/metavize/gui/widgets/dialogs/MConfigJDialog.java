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
import com.metavize.gui.widgets.dialogs.*;
import com.metavize.gui.widgets.coloredTable.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;

import com.metavize.mvvm.security.PasswordUtil;
import com.metavize.mvvm.*;

import javax.swing.*;
import javax.swing.text.*;
import java.text.*;
import java.util.*;
import javax.swing.table.*;
import java.awt.*;
import javax.swing.event.*;


/**
 *
 * @author  inieves
 */
public abstract class MConfigJDialog extends javax.swing.JDialog implements java.awt.event.WindowListener {

    
    // SAVING/REFRESHING ///////////
    protected Map<String, Refreshable> refreshableMap = new LinkedHashMap(5);
    protected Map<String, Savable> savableMap = new LinkedHashMap(5);
    protected Object settings;
    ///////////////////////////////


    public MConfigJDialog() {
        super(Util.getMMainJFrame(), true);
	this.generateButtonText();
        this.initComponents();
        this.setBounds( Util.generateCenteredBounds( Util.getMMainJFrame().getBounds(), this.getWidth(), this.getHeight()) );
        this.addWindowListener(this);   

        this.addComponentListener( 
				  new java.awt.event.ComponentAdapter() {
				      public void componentResized(java.awt.event.ComponentEvent evt) {
					  dialogResized();
				      }
				  } );
	generateGui();
	refreshAll();
    }

    // BUTTON STRINGS //////////////////////////////////
    protected ImageIcon RELOAD_INIT_STRING;
    protected ImageIcon RELOAD_ACTION_STRING;
    protected ImageIcon SAVE_INIT_STRING;
    protected ImageIcon SAVE_ACTION_STRING;

    protected void generateButtonText(){	
	RELOAD_INIT_STRING = Util.getButtonReloadSettings();
	RELOAD_ACTION_STRING = Util.getButtonReloading();
	SAVE_INIT_STRING = Util.getButtonSaveSettings();
	SAVE_ACTION_STRING = Util.getButtonSaving();
    }
    ////////////////////////////////////////////////////

    // SAVING/REFRESHING ///////////////////////////////
    protected abstract void sendSettings(Object settings) throws Exception;
    protected abstract void refreshSettings();
    protected abstract void generateGui();

    
    protected void saveAll(){
	// GENERATE AND VALIDATE ALL SETTINGS
	String componentName = null;
        try {
	    for( Map.Entry<String, Savable> savableMapEntry : savableMap.entrySet() ){
		componentName = savableMapEntry.getKey();
		Savable savableComponent = savableMapEntry.getValue();
		savableComponent.doSave(settings, false);
	    }
        }
        catch(Exception e){
            Util.handleExceptionNoRestart("Error preparing settings for saving", e);
            new ValidateFailureDialog( this.getTitle(), componentName, e.getMessage() );
            return;
        }
        
	// SEND SETTINGS TO SERVER
        try {
	    sendSettings(settings);
        }
        catch ( Exception e ) {
            try{
                Util.handleExceptionWithRestart("Error saving settings", e);
            }
            catch(Exception f){
                Util.handleExceptionNoRestart("Error saving settings", f);
                new SaveFailureDialog( this.getTitle() );
                return;
            }
        }
	finally{
	    refreshAll();
	}
    }

    protected void refreshAll(){
	// REFRESH SETTINGS FROM SERVER
	try{
	    refreshSettings();
	    for( Map.Entry<String, Refreshable> refreshableMapEntry : refreshableMap.entrySet() ){
		String componentName = refreshableMapEntry.getKey();
		Refreshable refreshableComponent = refreshableMapEntry.getValue();
		refreshableComponent.doRefresh(settings);
	    }
	}
	catch(Exception e){
            Util.handleExceptionNoRestart("Error preparing settings for refreshing", e);
	    new RefreshFailureDialog( this.getTitle() );
	}

    }
    ////////////////////////////////////////////


    // SIZING ///////////////////////////////
    protected Dimension MIN_SIZE = new Dimension(640, 480);
    protected Dimension MAX_SIZE = new Dimension(1600, 1200);

    private void dialogResized(){
        Util.resizeCheck(this, MIN_SIZE, MAX_SIZE);
    }
    ////////////////////////////////////////


    public void removeActionButtons(){
	reloadJButton.setVisible(false);
	saveJButton.setVisible(false);
    }


    
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
        closeJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/metavize/gui/images/Button_Close_Window_106x17.png")));
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
        reloadJButton.setIcon(RELOAD_INIT_STRING);
        reloadJButton.setDoubleBuffered(true);
        reloadJButton.setFocusPainted(false);
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
        saveJButton.setIcon(SAVE_INIT_STRING);
        saveJButton.setDoubleBuffered(true);
        saveJButton.setFocusPainted(false);
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

        java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        setBounds((screenSize.width-640)/2, (screenSize.height-480)/2, 640, 480);
    }//GEN-END:initComponents

    private void saveJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveJButtonActionPerformed
        new SaveAllThread();
    }//GEN-LAST:event_saveJButtonActionPerformed

    private void reloadJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reloadJButtonActionPerformed
        new RefreshAllThread();
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
            saveJButton.setEnabled(false);
            reloadJButton.setEnabled(false);
            closeJButton.setEnabled(false);
	    saveJButton.setIcon(SAVE_ACTION_STRING);
            this.start();
        }
        
        public void run(){
            try{
                MConfigJDialog.this.saveAll();
            }
            catch(Exception e){
                Util.handleExceptionNoRestart("Error saving settings", e);
            }
            finally{
                saveJButton.setEnabled(true);
                reloadJButton.setEnabled(true);
                closeJButton.setEnabled(true);
		saveJButton.setIcon(SAVE_INIT_STRING);
            }
        }
    }
    
    
    private class RefreshAllThread extends Thread{
        public RefreshAllThread(){
	    super("MVCLIENT-MConfigJDialog.RefreshAllThread");
            saveJButton.setEnabled(false);
            reloadJButton.setEnabled(false);
            closeJButton.setEnabled(false);
	    reloadJButton.setIcon(RELOAD_ACTION_STRING);
            this.start();
        }
        
        public void run(){
            try{
                MConfigJDialog.this.refreshAll();
            }
            catch(Exception e){
                Util.handleExceptionNoRestart("Error saving settings", e);
            }
            finally{
                saveJButton.setEnabled(true);
                reloadJButton.setEnabled(true);
                closeJButton.setEnabled(true);
		reloadJButton.setIcon(RELOAD_INIT_STRING);
            }
        }
    }
}







