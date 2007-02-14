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


import com.untangle.gui.widgets.editTable.*;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.widgets.*;
import com.untangle.gui.util.*;

import com.untangle.mvvm.*;
import com.untangle.mvvm.tran.*;

import org.netbeans.lib.awtextra.AbsoluteConstraints;
import org.netbeans.lib.awtextra.AbsoluteLayout;
import java.util.*;
import java.awt.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.net.URL;
import javax.jnlp.BasicService;
import javax.jnlp.ServiceManager;

public abstract class MTransformControlsJPanel extends javax.swing.JPanel implements SettingsChangedListener{

    // SAVING/REFRESHING/SHUTDOWN //////////
    private Map<String, Refreshable> refreshableMap = new LinkedHashMap(5);
    protected void addRefreshable(String name, Refreshable refreshable){ refreshableMap.put(name, refreshable); }
    protected void removeRefreshable(String refreshableKey){ refreshableMap.remove(refreshableKey); }
    private Map<String, Savable> savableMap = new LinkedHashMap(5);
    protected void addSavable(String name, Savable savable){ savableMap.put(name, savable); }
    protected void removeSavable(String savableKey){ savableMap.remove(savableKey); }
    protected Object settings;
    private InfiniteProgressJComponent infiniteProgressJComponent = new InfiniteProgressJComponent();
    public  InfiniteProgressJComponent getInfiniteProgressJComponent(){ return infiniteProgressJComponent; }
    public static final long MIN_PROGRESS_MILLIS = 1500;
    public void addShutdownable(String name, Shutdownable shutdownable){ mTransformJPanel.addShutdownable(name, shutdownable); }
    public void removeShutdownable(String shutdownableKey){ mTransformJPanel.removeShutdownable(shutdownableKey); }
    ///////////////////////////////
    
    // EXPANDING/CONTACTING //////
    protected Dimension MIN_SIZE = new Dimension(640, 480);
    protected Dimension MAX_SIZE = new Dimension(1600, 1200);
    private AbsoluteConstraints oldConstraints;
    private JDialog expandJDialog;
    private static final int EXPAND_WIDTH = 700;
    private static final int EXPAND_HEIGHT = 500;
    private static final int EXPAND_INSET = 75;
    private static GridBagConstraints greyBackgroundConstraints = new GridBagConstraints(0,0,1,1,1d,1d,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0);
    private static GridBagConstraints contentConstraints = new GridBagConstraints(0,0,1,1,1d,1d,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(15,15,15,15), 0, 0);
    private static GridBagConstraints infiniteConstraints = new GridBagConstraints(0,0,1,1,1d,1d,GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0);
    private static ImageIcon greyBackgroundImageIcon;
    //////////////////////////////

    // HELPER ////////
    private static final int HELPER_SAVE_SETTINGS_BLINK = 200;
    private static ImageIcon[] saveSettingsImageIcons;
    CycleJLabel saveSettingsHintJLabel;

    protected MTransformJPanel mTransformJPanel;    

    public MTransformControlsJPanel(MTransformJPanel mTransformJPanel) {
	//setDoubleBuffered(true);
        this.mTransformJPanel = mTransformJPanel;

	// HELPER //
	synchronized( this ){
	    if( saveSettingsImageIcons == null ){
		String[] saveSettingsImagePaths = { "com/untangle/gui/transform/IconSaveSettingsHint30.png",
						    "com/untangle/gui/transform/IconSaveSettingsHint40.png",
						    "com/untangle/gui/transform/IconSaveSettingsHint50.png",
						    "com/untangle/gui/transform/IconSaveSettingsHint60.png",
						    "com/untangle/gui/transform/IconSaveSettingsHint70.png",
						    "com/untangle/gui/transform/IconSaveSettingsHint80.png",
						    "com/untangle/gui/transform/IconSaveSettingsHint90.png",
						    "com/untangle/gui/transform/IconSaveSettingsHint100.png" };
		saveSettingsImageIcons = Util.getImageIcons(saveSettingsImagePaths);
	    }
	}
	saveSettingsHintJLabel = new CycleJLabel(saveSettingsImageIcons, HELPER_SAVE_SETTINGS_BLINK, true, true);

        // INITIALIZE GUI
        initComponents();
        helpJButton.setVisible(false);
	add(infiniteProgressJComponent, infiniteConstraints, 0);

	mTabbedPane.setFont( new java.awt.Font("Arial", 0, 14) );


        // SETUP EXPAND DIALOG
	if(greyBackgroundImageIcon == null)
	    greyBackgroundImageIcon = new javax.swing.ImageIcon( Util.getClassLoader().getResource("com/untangle/gui/images/DarkGreyBackground1600x100.png"));
        expandJDialog = new JDialog( Util.getMMainJFrame(), true);
        expandJDialog.setSize(MIN_SIZE);
	expandJDialog.addComponentListener( new ComponentAdapter() { public void componentResized(ComponentEvent evt) {
	    dialogResized();
	}});
        expandJDialog.getContentPane().setLayout(new GridBagLayout());
	expandJDialog.getContentPane().add(new com.untangle.gui.widgets.MTiledIconLabel("",greyBackgroundImageIcon,JLabel.CENTER), greyBackgroundConstraints);
    
    }

    public boolean isOptimizedDrawingEnabled(){
	return false;
    }

    public void settingsChanged(Object source){
	setSaveSettingsHintVisible(true);
    }

    public void doShutdown(){
	collapseControlPanel();
    }
    
    private void dialogResized(){
        Util.resizeCheck(expandJDialog, MIN_SIZE, MAX_SIZE);
    }
    
    public void collapseControlPanel(){
	if( expandJDialog.isVisible() )
	    expandJDialog.setVisible(false);
    }
    
    public boolean getControlsShowing(){
	return mTransformJPanel.getControlsShowing();
    }

    public void setSaveSettingsHintVisible(boolean isVisible){
	if( isVisible ){
	    if( !saveSettingsHintJLabel.isRunning() )
		saveSettingsHintJLabel.start();
	}
	else
	    saveSettingsHintJLabel.stop();
    }

    // TABBED PANE ////////
    public JTabbedPane getMTabbedPane(){ return mTabbedPane; }
    protected void removeTab(String s){
	int index = mTabbedPane.indexOfTab(s);
	if(index >= 0)
	    mTabbedPane.remove(index);
    }
    protected void addTab(String title, Icon icon, Component component){ addTab(mTabbedPane.getTabCount(), title, icon, component); }
    protected void addTab(int index, String title, Icon icon, Component component){ mTabbedPane.insertTab(title, icon, component, null, index); }
    protected JTabbedPane addTabbedPane(String name, Icon icon){ return addTabbedPane(mTabbedPane.getTabCount(), name, icon); }
    protected JTabbedPane addTabbedPane(int index, String name, Icon icon){
        JTabbedPane newJTabbedPane = new JTabbedPane();
        newJTabbedPane.setBorder(new EmptyBorder(7, 13, 13, 13));
        newJTabbedPane.setFocusable(false);
        newJTabbedPane.setFont(new java.awt.Font("Arial", 0, 11));
	newJTabbedPane.setRequestFocusEnabled(false);
	JPanel backJPanel = new JPanel();
	backJPanel.setOpaque(true);
	backJPanel.setLayout(new BorderLayout());
	backJPanel.add(newJTabbedPane);
	addTab(index, name, icon, backJPanel);
	return newJTabbedPane;
    }
    protected JScrollPane addScrollableTab(JTabbedPane parentJTabbedPane, String name, Icon icon,
					   Component childComponent, boolean scrollH, boolean scrollV){
	return addScrollableTab(mTabbedPane.getTabCount(), parentJTabbedPane, name, icon, childComponent, scrollH, scrollV);
    }
    protected JScrollPane addScrollableTab(int index, JTabbedPane parentJTabbedPane, String name, Icon icon,
					   Component childComponent, boolean scrollH, boolean scrollV){
	final JScrollPane newJScrollPane = new JScrollPane(childComponent);
	newJScrollPane.setHorizontalScrollBarPolicy( scrollH ? JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS : JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
	newJScrollPane.setVerticalScrollBarPolicy( scrollV ? JScrollPane.VERTICAL_SCROLLBAR_ALWAYS : JScrollPane.VERTICAL_SCROLLBAR_NEVER );
	if( parentJTabbedPane != null )
	    parentJTabbedPane.addTab(name, icon, newJScrollPane);
	else
	    addTab(index, name, icon, newJScrollPane);
	SwingUtilities.invokeLater( new Runnable(){ public void run(){
	    newJScrollPane.getVerticalScrollBar().setValue(0);
	    newJScrollPane.getVerticalScrollBar().setUnitIncrement(5);
	}});
	return newJScrollPane;
    }
    /////////////////////////

    public JToggleButton getControlsJToggleButton(){ return mTransformJPanel.getControlsJToggleButton(); }
    
    
    public void setAllEnabled(boolean enabled){
        int innerComponentCount, outerComponentCount;
        Component innerComponent, outerComponent;
        
        outerComponentCount = mTabbedPane.getComponentCount();
                
        for(int i=0; i<outerComponentCount; i++){
            outerComponent = mTabbedPane.getComponentAt(i);
            if( outerComponent instanceof MEditTableJPanel){
                ((MEditTableJPanel)outerComponent).setAllEnabled(enabled);
            }
            else if( (outerComponent instanceof JTabbedPane) ) {
                innerComponentCount = ((JTabbedPane)outerComponent).getComponentCount();
                for(int j=0; j<innerComponentCount; j++){
                    innerComponent = ((JTabbedPane)outerComponent).getComponentAt(j);
                    if( innerComponent instanceof MEditTableJPanel){
                        ((MEditTableJPanel)innerComponent).setAllEnabled(enabled);
                    }
                }
            }
        }
    }

    protected boolean shouldSave(){ return true; }
    
    public abstract void generateGui();

    private Exception saveException;
    public void saveAll() throws Exception {
	// GENERATE AND VALIDATE ALL SETTINGS
	final String transformName = mTransformJPanel.getMackageDesc().getDisplayName();
	for( Map.Entry<String, Savable> savableMapEntry : savableMap.entrySet() ){
	    final String componentName = savableMapEntry.getKey();
	    final Savable savableComponent = savableMapEntry.getValue();
	    saveException = null;
	    SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
		try{ savableComponent.doSave(settings, false); }
		catch(Exception e){
		    saveException = e;
		}
	    }});
	    if( saveException != null ){
		ValidateFailureDialog.factory( (Window) MTransformControlsJPanel.this.contentJPanel.getTopLevelAncestor(),
					       transformName, componentName, saveException.getMessage() );
		throw new ValidationException();
	    }
	}
	if( settings instanceof Validatable ){
	    try{ ((Validatable)settings).validate(); }
	    catch(Exception e){
		ValidateFailureDialog.factory( (Window) MTransformControlsJPanel.this.contentJPanel.getTopLevelAncestor(),
					       transformName, "multiple settings panels", e.getMessage() );
		throw new ValidationException();
	    }
	}        
	// SEND SETTINGS TO SERVER
	mTransformJPanel.getTransform().setSettings( settings );
	setSaveSettingsHintVisible(false);
    }

    public void saveGui(){
        SwingUtilities.invokeLater( new Runnable(){ public void run(){
            saveJButton.doClick();
        }});
    }
    


    public void refreshGui(){
	SwingUtilities.invokeLater( new Runnable(){ public void run(){
	    reloadJButton.doClick();
	}});
    }

    public void refreshAll() throws Exception {
	settings = mTransformJPanel.getTransform().getSettings();
    }

    void populateAll() throws Exception {
	final String transformName = mTransformJPanel.getMackageDesc().getDisplayName();
	// SEND SETTINGS TO EACH PANEL, SERIALLY, INDEPENDANTLY
	for( Map.Entry<String, Refreshable> refreshableMapEntry : refreshableMap.entrySet() ){
	    final Refreshable refreshableComponent = refreshableMapEntry.getValue();
	    SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
		try{ refreshableComponent.doRefresh(settings); }
		catch(Exception e){
		    Util.handleExceptionNoRestart("Error distributing settings",e);
		    RefreshFailureDialog.factory( (Window) MTransformControlsJPanel.this.contentJPanel.getTopLevelAncestor(),
						  transformName );
		}
	    }});
	}
	setSaveSettingsHintVisible(false);
    }
    


    public JPanel getContentJPanel(){ return contentJPanel; }
    public JButton saveJButton(){ return saveJButton; }
    public JButton reloadJButton(){ return reloadJButton; }
    public JButton removeJButton(){ return removeJButton; }
        private void initComponents() {//GEN-BEGIN:initComponents
                java.awt.GridBagConstraints gridBagConstraints;

                stateButtonGroup = new javax.swing.ButtonGroup();
                socketJPanel = new javax.swing.JPanel();
                contentJPanel = new javax.swing.JPanel();
                mTabbedPane = new javax.swing.JTabbedPane();
                nbSaveSettingsHintJLabel = saveSettingsHintJLabel;
                removeJButton = new javax.swing.JButton();
                helpJButton = new javax.swing.JButton();
                expandJButton = new javax.swing.JButton();
                reloadJButton = new javax.swing.JButton();
                saveJButton = new javax.swing.JButton();
                readoutJLabel = new javax.swing.JLabel();
                backgroundJLabel = new com.untangle.gui.widgets.MTiledIconLabel();

                setLayout(new java.awt.GridBagLayout());

                setMaximumSize(new java.awt.Dimension(596, 404));
                setMinimumSize(new java.awt.Dimension(596, 404));
                setPreferredSize(new java.awt.Dimension(596, 404));
                socketJPanel.setLayout(new java.awt.BorderLayout());

                socketJPanel.setOpaque(false);
                contentJPanel.setLayout(new java.awt.GridBagLayout());

                contentJPanel.setOpaque(false);
                mTabbedPane.setDoubleBuffered(true);
                mTabbedPane.setFocusable(false);
                mTabbedPane.setFont(new java.awt.Font("Arial", 0, 11));
                mTabbedPane.setRequestFocusEnabled(false);
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 15, 0);
                contentJPanel.add(mTabbedPane, gridBagConstraints);

                nbSaveSettingsHintJLabel.setFont(new java.awt.Font("Arial", 0, 18));
                nbSaveSettingsHintJLabel.setForeground(new java.awt.Color(255, 0, 0));
                nbSaveSettingsHintJLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
                nbSaveSettingsHintJLabel.setDoubleBuffered(true);
                nbSaveSettingsHintJLabel.setFocusable(false);
                nbSaveSettingsHintJLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
                nbSaveSettingsHintJLabel.setIconTextGap(0);
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.gridheight = 2;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHEAST;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 21, 55);
                contentJPanel.add(nbSaveSettingsHintJLabel, gridBagConstraints);

                removeJButton.setFont(new java.awt.Font("Arial", 0, 12));
                removeJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/Button_Remove_Appliance_106x17.png")));
                removeJButton.setDoubleBuffered(true);
                removeJButton.setFocusPainted(false);
                removeJButton.setFocusable(false);
                removeJButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
                removeJButton.setIconTextGap(0);
                removeJButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
                removeJButton.setMaximumSize(new java.awt.Dimension(130, 25));
                removeJButton.setMinimumSize(new java.awt.Dimension(130, 25));
                removeJButton.setOpaque(false);
                removeJButton.setPreferredSize(new java.awt.Dimension(130, 25));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                contentJPanel.add(removeJButton, gridBagConstraints);

                helpJButton.setIcon(new javax.swing.ImageIcon( Util.getClassLoader().getResource("com/untangle/gui/transform/IconHelp28x28.png")));
                helpJButton.setText("Help");
                helpJButton.setFocusPainted(false);
                helpJButton.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
                helpJButton.setIconTextGap(2);
                helpJButton.setMargin(new java.awt.Insets(0, 0, 0, 9));
                helpJButton.setMaximumSize(new java.awt.Dimension(86, 25));
                helpJButton.setMinimumSize(new java.awt.Dimension(86, 25));
                helpJButton.setOpaque(false);
                helpJButton.setPreferredSize(new java.awt.Dimension(86, 25));
                helpJButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                helpJButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                contentJPanel.add(helpJButton, gridBagConstraints);

                expandJButton.setFont(new java.awt.Font("Arial", 0, 12));
                expandJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/Button_Expand_Settings_106x17.png")));
                expandJButton.setDoubleBuffered(true);
                expandJButton.setFocusPainted(false);
                expandJButton.setFocusable(false);
                expandJButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
                expandJButton.setIconTextGap(0);
                expandJButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
                expandJButton.setMaximumSize(new java.awt.Dimension(140, 25));
                expandJButton.setMinimumSize(new java.awt.Dimension(140, 25));
                expandJButton.setOpaque(false);
                expandJButton.setPreferredSize(new java.awt.Dimension(140, 25));
                expandJButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                expandJButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(0, 160, 0, 0);
                contentJPanel.add(expandJButton, gridBagConstraints);

                reloadJButton.setFont(new java.awt.Font("Arial", 0, 12));
                reloadJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/Button_Reload_Settings_106x17.png")));
                reloadJButton.setDoubleBuffered(true);
                reloadJButton.setFocusPainted(false);
                reloadJButton.setFocusable(false);
                reloadJButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
                reloadJButton.setIconTextGap(0);
                reloadJButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
                reloadJButton.setMaximumSize(new java.awt.Dimension(115, 25));
                reloadJButton.setMinimumSize(new java.awt.Dimension(115, 25));
                reloadJButton.setOpaque(false);
                reloadJButton.setPreferredSize(new java.awt.Dimension(115, 25));
                reloadJButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                reloadJButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 119);
                contentJPanel.add(reloadJButton, gridBagConstraints);

                saveJButton.setFont(new java.awt.Font("Arial", 0, 12));
                saveJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/Button_Save_Settings_106x17.png")));
                saveJButton.setDoubleBuffered(true);
                saveJButton.setFocusPainted(false);
                saveJButton.setFocusable(false);
                saveJButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
                saveJButton.setIconTextGap(0);
                saveJButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
                saveJButton.setMaximumSize(new java.awt.Dimension(115, 25));
                saveJButton.setMinimumSize(new java.awt.Dimension(115, 25));
                saveJButton.setOpaque(false);
                saveJButton.setPreferredSize(new java.awt.Dimension(115, 25));
                saveJButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                saveJButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                contentJPanel.add(saveJButton, gridBagConstraints);

                socketJPanel.add(contentJPanel, java.awt.BorderLayout.CENTER);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(7, 13, 20, 13);
                add(socketJPanel, gridBagConstraints);

                readoutJLabel.setFont(new java.awt.Font("Default", 0, 12));
                readoutJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                readoutJLabel.setText("Settings expanded...");
                readoutJLabel.setBorder(new javax.swing.border.EtchedBorder());
                readoutJLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
                readoutJLabel.setIconTextGap(0);
                readoutJLabel.setMaximumSize(new java.awt.Dimension(200, 200));
                readoutJLabel.setMinimumSize(new java.awt.Dimension(200, 200));
                readoutJLabel.setPreferredSize(new java.awt.Dimension(200, 200));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                add(readoutJLabel, gridBagConstraints);

                backgroundJLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/LightGreyBackground1600x100.png")));
                backgroundJLabel.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(128, 127, 127)));
                backgroundJLabel.setDoubleBuffered(true);
                backgroundJLabel.setMaximumSize(new java.awt.Dimension(596, 380));
                backgroundJLabel.setMinimumSize(new java.awt.Dimension(596, 380));
                backgroundJLabel.setPreferredSize(new java.awt.Dimension(596, 380));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                add(backgroundJLabel, gridBagConstraints);

        }//GEN-END:initComponents

		private void helpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_helpJButtonActionPerformed
            try{
                String focus = Util.getSelectedTabTitle(mTabbedPane).toLowerCase().replace(" ", "_");
                String source = mTransformJPanel.getTransformDesc().getDisplayName().toLowerCase().replace(" ", "_");
                URL newURL = new URL( "http://www.untangle.com/docs?"
                                      + "version=" + Version.getVersion()
                                      + "&source=" + source
                                      + "&focus=" + focus);
                ((BasicService) ServiceManager.lookup("javax.jnlp.BasicService")).showDocument(newURL);
            }
            catch(Exception f){
                Util.handleExceptionNoRestart("Error showing help for " + mTransformJPanel.getTransformDesc().getDisplayName(), f);
            }
		}//GEN-LAST:event_helpJButtonActionPerformed

    private void saveJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveJButtonActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_saveJButtonActionPerformed

    private void reloadJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reloadJButtonActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_reloadJButtonActionPerformed

    private void expandJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_expandJButtonActionPerformed
	if( !MTransformControlsJPanel.this.expandJDialog.isVisible() ){
	    // change layout
        helpJButton.setVisible(true);
        removeJButton.setVisible(false);
	    remove(infiniteProgressJComponent);
            socketJPanel.remove(contentJPanel);
            revalidate();
	    repaint();
	    expandJDialog.setGlassPane(infiniteProgressJComponent);
            expandJDialog.getContentPane().add(contentJPanel, contentConstraints, 0);

            // place new window in the center of parent window and show
            expandJDialog.setBounds( Util.generateCenteredBounds(Util.getMMainJFrame().getBounds(),
								 Util.getMMainJFrame().getWidth()-EXPAND_INSET,
								 Util.getMMainJFrame().getHeight()-EXPAND_INSET) );
	    expandJButton.setIcon(Util.getButtonCollapseSettings());
	    expandJDialog.setTitle( mTransformJPanel.getTransformDesc().getDisplayName() + " (expanded settings window)");
            expandJDialog.setVisible(true);

            // cleanup after new window is closed
            helpJButton.setVisible(false);
            removeJButton.setVisible(true);
	    expandJButton.setIcon(Util.getButtonExpandSettings());
            expandJDialog.getContentPane().remove(contentJPanel);
            socketJPanel.add(contentJPanel);
	    add(infiniteProgressJComponent, infiniteConstraints, 0);
            revalidate();
	    repaint();
	}
	else{
	    expandJDialog.setVisible(false);
	}
    }//GEN-LAST:event_expandJButtonActionPerformed
    
    
        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JLabel backgroundJLabel;
        protected javax.swing.JPanel contentJPanel;
        protected javax.swing.JButton expandJButton;
        private javax.swing.JButton helpJButton;
        protected javax.swing.JTabbedPane mTabbedPane;
        protected javax.swing.JLabel nbSaveSettingsHintJLabel;
        protected javax.swing.JLabel readoutJLabel;
        protected javax.swing.JButton reloadJButton;
        protected javax.swing.JButton removeJButton;
        protected javax.swing.JButton saveJButton;
        protected javax.swing.JPanel socketJPanel;
        private javax.swing.ButtonGroup stateButtonGroup;
        // End of variables declaration//GEN-END:variables



}





  
