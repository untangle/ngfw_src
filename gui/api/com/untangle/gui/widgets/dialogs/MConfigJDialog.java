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

package com.untangle.gui.widgets.dialogs;

import com.untangle.gui.transform.*;
import com.untangle.gui.widgets.coloredTable.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.gui.util.*;

import com.untangle.mvvm.security.PasswordUtil;
import com.untangle.mvvm.*;
import com.untangle.mvvm.tran.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.text.*;
import java.util.*;
import java.net.URL;
import javax.jnlp.BasicService;
import javax.jnlp.ServiceManager;

public abstract class MConfigJDialog extends javax.swing.JDialog implements java.awt.event.WindowListener {
    
    // SAVING/REFRESHING ///////////
    protected boolean settingsSaved;
    public boolean getSettingsSaved(){ return settingsSaved; }
    protected boolean settingsChanged;
    public boolean getSettingsChanged(){ return settingsChanged; }
    private Map<String, Refreshable> refreshableMap = new LinkedHashMap(5);
    protected void addRefreshable(String name, Refreshable refreshable){ refreshableMap.put(name, refreshable); }
    private Map<String, Savable> savableMap = new LinkedHashMap(5);
    protected void addSavable(String name, Savable savable){ savableMap.put(name, savable); }
    private Map<String, Shutdownable> shutdownableMap = new LinkedHashMap(1);
    protected void addShutdownable(String name, Shutdownable shutdownable){ shutdownableMap.put(name, shutdownable); }
    protected CompoundSettings compoundSettings;
    public CompoundSettings getCompoundSettings(){ return compoundSettings; }
    protected InfiniteProgressJComponent infiniteProgressJComponent = new InfiniteProgressJComponent();
    public InfiniteProgressJComponent getInfiniteProgressJComponent(){ return infiniteProgressJComponent; }
    private static final long MIN_PROGRESS_MILLIS = 1000;
    ///////////////////////////////

    public MConfigJDialog(Dialog parentDialog){
	super(parentDialog, true);
	init(parentDialog);
    }

    public MConfigJDialog(Frame parentFrame) {
	super(parentFrame, true);
	init(parentFrame);
    }
    
    private void init(Window parentWindow){
        getRootPane().setDoubleBuffered(true);
        RepaintManager.currentManager(this).setDoubleBufferingEnabled(true);
        initComponents();
        helpJButton.setVisible(false);
        setBounds( Util.generateCenteredBounds( parentWindow.getBounds(), getMinSize().width, getMinSize().height) );
        addWindowListener(this);
        setGlassPane(infiniteProgressJComponent);
        addComponentListener( new ComponentAdapter() { public void componentResized(ComponentEvent evt) {
            dialogResized();
        }});
    }

    private static JComponent initialFocusComponent;
    public static void setInitialFocusComponent(JComponent c){
        initialFocusComponent = c;
    }
    public static JComponent getInitialFocusComponent(){ return initialFocusComponent; }

    public void setVisible(boolean isVisible){
        if(isVisible){
            new RefreshAllThread(true);
        }
        super.setVisible(isVisible);
    }

    private String helpSource;
    public void setHelpSource(String helpSource){
        this.helpSource = helpSource;
        helpJButton.setVisible(true);
    }

    protected abstract void generateGui();

    // TABS AND TABBED PANES //////////////
    public JTabbedPane getMTabbedPane(){ return contentJTabbedPane; }
    public void addTab(String name, Icon icon, Component component){ contentJTabbedPane.addTab(name, icon, component); }
    public JTabbedPane addTabbedPane(String name, Icon icon){
        JTabbedPane newJTabbedPane = new JTabbedPane();
        newJTabbedPane.setBorder(new EmptyBorder(7, 13, 13, 13));
        newJTabbedPane.setFont(new java.awt.Font("Arial", 0, 11));
        JPanel backJPanel = new JPanel();
        backJPanel.setLayout(new BorderLayout());
        backJPanel.add(newJTabbedPane);
        addTab(name, icon, backJPanel);
        return newJTabbedPane;
    }
    public JScrollPane addScrollableTab(JTabbedPane parentJTabbedPane, String name, Icon icon,
					Component childComponent, boolean scrollH, boolean scrollV){
	JScrollPane newJScrollPane = new JScrollPane(childComponent);
    newJScrollPane.getHorizontalScrollBar().setFocusable(false);
    newJScrollPane.getVerticalScrollBar().setFocusable(false);
	newJScrollPane.setHorizontalScrollBarPolicy( scrollH ? JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS : JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
	newJScrollPane.setVerticalScrollBarPolicy( scrollV ? JScrollPane.VERTICAL_SCROLLBAR_ALWAYS : JScrollPane.VERTICAL_SCROLLBAR_NEVER );
	if( parentJTabbedPane != null )
	    parentJTabbedPane.addTab(name, icon, newJScrollPane);
	else
	    addTab(name, icon, newJScrollPane);
	newJScrollPane.getVerticalScrollBar().setUnitIncrement(5);
	return newJScrollPane;
    }
    //////////////////////////////////////

    protected boolean shouldSave(){ return true; }

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
	    if( compoundSettings != null )
		compoundSettings.validate();
	}
	catch(Exception e){
	    ValidateFailureDialog.factory( (Window) MConfigJDialog.this,
					   getTitle(), "multiple settings panels", e.getMessage() );
	    throw new ValidationException();
	}
	// SEND SETTINGS TO SERVER
	if( compoundSettings != null )
	    compoundSettings.save();

	// RECORD THE FACT THAT SETTINGS WERE SAVED FOR WHEN THE DIALOG RETURNS
	settingsSaved = true;
    }


    protected void refreshAll() throws Exception {
	// GET SETTINGS FROM SERVER
	if( compoundSettings != null )
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
                helpJButton = new javax.swing.JButton();
                reloadJButton = new javax.swing.JButton();
                saveJButton = new javax.swing.JButton();
                backgroundJLabel = new com.untangle.gui.widgets.MTiledIconLabel();

                getContentPane().setLayout(new java.awt.GridBagLayout());

                setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
                setTitle("");
                setModal(true);
                contentJTabbedPane.setFont(new java.awt.Font("Default", 0, 12));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.gridwidth = 4;
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(15, 15, 15, 15);
                getContentPane().add(contentJTabbedPane, gridBagConstraints);

                closeJButton.setFont(new java.awt.Font("Default", 0, 12));
                closeJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/IconClose_16x16.png")));
                closeJButton.setText("Close");
                closeJButton.setIconTextGap(6);
                closeJButton.setMargin(new java.awt.Insets(2, 6, 2, 6));
                closeJButton.setOpaque(false);
                closeJButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                closeJButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHWEST;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 15, 15, 0);
                getContentPane().add(closeJButton, gridBagConstraints);

                helpJButton.setFont(new java.awt.Font("Dialog", 0, 12));
                helpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/IconHelp_18x16.png")));
                helpJButton.setText("Help");
                helpJButton.setIconTextGap(6);
                helpJButton.setMargin(new java.awt.Insets(2, 6, 2, 6));
                helpJButton.setMaximumSize(null);
                helpJButton.setMinimumSize(null);
                helpJButton.setOpaque(false);
                helpJButton.setPreferredSize(null);
                helpJButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                helpJButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHWEST;
                gridBagConstraints.insets = new java.awt.Insets(0, 170, 15, 0);
                getContentPane().add(helpJButton, gridBagConstraints);

                reloadJButton.setFont(new java.awt.Font("Arial", 0, 12));
                reloadJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/IconCancel_16x16.png")));
                reloadJButton.setText("Cancel");
                reloadJButton.setIconTextGap(6);
                reloadJButton.setMargin(new java.awt.Insets(2, 6, 2, 6));
                reloadJButton.setOpaque(false);
                reloadJButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                reloadJButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 2;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHEAST;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 15, 15);
                getContentPane().add(reloadJButton, gridBagConstraints);

                saveJButton.setFont(new java.awt.Font("Arial", 0, 12));
                saveJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/IconSave_23x16.png")));
                saveJButton.setText("Save");
                saveJButton.setIconTextGap(6);
                saveJButton.setMargin(new java.awt.Insets(2, 6, 2, 6));
                saveJButton.setOpaque(false);
                saveJButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                saveJButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 3;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHEAST;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 15, 15);
                getContentPane().add(saveJButton, gridBagConstraints);

                backgroundJLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/DarkGreyBackground1600x100.png")));
                backgroundJLabel.setFocusable(false);
                backgroundJLabel.setOpaque(true);
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.gridwidth = 4;
                gridBagConstraints.gridheight = 2;
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                getContentPane().add(backgroundJLabel, gridBagConstraints);

        }//GEN-END:initComponents

		private void helpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_helpJButtonActionPerformed
            try{
                String focus = Util.getSelectedTabTitle(contentJTabbedPane).toLowerCase().replace(" ", "_");
                URL newURL = new URL( "http://www.untangle.com/docs/get.php?"
                                      + "version=" + Version.getVersion()
                                      + "&source=" + helpSource
                                      + "&focus=" + focus);
                ((BasicService) ServiceManager.lookup("javax.jnlp.BasicService")).showDocument(newURL);
            }
            catch(Exception f){
                Util.handleExceptionNoRestart("Error showing help for " + helpSource, f);
            }
		}//GEN-LAST:event_helpJButtonActionPerformed

    private void saveJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveJButtonActionPerformed
	if( Util.getIsDemo() )
	    return;
	if( !shouldSave() )
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
        private javax.swing.JButton helpJButton;
        protected javax.swing.JButton reloadJButton;
        protected javax.swing.JButton saveJButton;
        // End of variables declaration//GEN-END:variables

    private class SaveAllThread extends Thread{
        public SaveAllThread(){
	    super("MVCLIENT-ConfigSaveAllThread");
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
	    super("MVCLIENT-ConfigRefreshAllThread");
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
        // FOCUS
        SwingUtilities.invokeLater( new Runnable(){ public void run(){
            JComponent target = MConfigJDialog.getInitialFocusComponent();
            if(target != null){
                target.requestFocus();
                if(target instanceof JTextComponent)
                    ((JTextComponent)target).selectAll();
            }
        }});

        }
    }
}







