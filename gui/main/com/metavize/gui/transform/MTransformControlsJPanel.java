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


import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.widgets.dialogs.*;
import com.metavize.gui.util.*;

import com.metavize.mvvm.*;
import com.metavize.mvvm.tran.*;

import org.netbeans.lib.awtextra.AbsoluteConstraints;
import org.netbeans.lib.awtextra.AbsoluteLayout;
import java.util.*;
import javax.swing.*;
import java.awt.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.util.*;

/**
 *
 * @author  root
 */
public class MTransformControlsJPanel extends javax.swing.JPanel {

    // SAVING/REFRESHING //////////
    protected Map<String, Refreshable> refreshableMap = new LinkedHashMap(5);
    protected Map<String, Savable> savableMap = new LinkedHashMap(5);
    protected Object settings;
    
    // EXPANDING/CONTACTING

    protected Dimension MIN_SIZE = new Dimension(800, 600);
    protected Dimension MAX_SIZE = new Dimension(1600, 1200);
    private AbsoluteConstraints oldConstraints;
    private JDialog expandJDialog;
    private static final int EXPAND_WIDTH = 700;
    private static final int EXPAND_HEIGHT = 500;
    private static final int EXPAND_INSET = 75;
    private static GridBagConstraints greyBackgroundConstraints = new GridBagConstraints(0,0,1,1,1d,1d,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0);
    private static GridBagConstraints contentConstraints = new GridBagConstraints(0,0,1,1,1d,1d,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(15,15,15,15), 0, 0);
    private static ImageIcon greyBackgroundImageIcon;


    protected MTransformJPanel mTransformJPanel;
    
    /** Creates new form TransformMinJPanel */
    public MTransformControlsJPanel(MTransformJPanel mTransformJPanel) {
        this.mTransformJPanel = mTransformJPanel;
        // INITIALIZE GUI
        initComponents();

	mTabbedPane.setFont( new java.awt.Font("Arial", 0, 14) );

        // SETUP EXPAND DIALOG
	if(greyBackgroundImageIcon == null)
	    greyBackgroundImageIcon = new javax.swing.ImageIcon( Util.getClassLoader().getResource("com/metavize/gui/images/DarkGreyBackground1600x100.png"));
        expandJDialog = new JDialog( Util.getMMainJFrame(), 
				     mTransformJPanel.getTransformContext().getMackageDesc().getDisplayName()
				     + " (expanded settings window)", true);
        expandJDialog.setSize(MIN_SIZE);
        expandJDialog.addComponentListener( 
            new java.awt.event.ComponentAdapter() {
                public void componentResized(java.awt.event.ComponentEvent evt) {
                    dialogResized();
                }
            } );
        expandJDialog.getContentPane().setLayout(new GridBagLayout());
	expandJDialog.getContentPane().add(new com.metavize.gui.widgets.MTiledIconLabel("",greyBackgroundImageIcon,JLabel.CENTER), greyBackgroundConstraints);
    

	generateGui();
	refreshAll();
    }
    
    private void dialogResized(){
        Util.resizeCheck(expandJDialog, MIN_SIZE, MAX_SIZE);
    }
    
    protected void postInit(){
        if(mTabbedPane.getTabCount() > 0)
            mTabbedPane.setSelectedIndex(0);
    }
    
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
    
    protected void generateGui(){};

    protected void saveAll(){

        if(Util.getIsDemo())
            return;

	// GENERATE AND VALIDATE ALL SETTINGS
	StringBuilder message = new StringBuilder();
	String componentName = "";
        try {
	    message.append("Saving: "+ mTransformJPanel.getTransformContext().getTransformDesc().getDisplayName() );
	    for( Map.Entry<String, Savable> savableMapEntry : savableMap.entrySet() ){
		componentName = savableMapEntry.getKey();
		Savable savableComponent = savableMapEntry.getValue();
		savableComponent.doSave(settings, false);
		message.append("\n  " + componentName);
	    }
            if( settings instanceof Validatable )
                ((Validatable)settings).validate();
        }
        catch(Exception e){
            new ValidateFailureDialog( mTransformJPanel.getTransformContext().getTransformDesc().getDisplayName(),
				       componentName, e.getMessage() );
	    return;
        }
        
	// SEND SETTINGS TO SERVER AND RECONFIGURE
        try {
	    mTransformJPanel.getTransformContext().transform().setSettings( settings );
            mTransformJPanel.getTransformContext().transform().reconfigure();
        }
        catch ( Exception e ) {
            try{
                Util.handleExceptionWithRestart("ERROR SAVING: " + mTransformJPanel.getTransformContext().getMackageDesc().getDisplayName(), e);
            }
            catch(Exception f){
                Util.handleExceptionNoRestart("ERROR SAVING: " + mTransformJPanel.getTransformContext().getMackageDesc().getDisplayName(), f);
                new SaveFailureDialog( mTransformJPanel.getTransformContext().getTransformDesc().getDisplayName() );
            }
        }
        finally{
	    Util.printMessage(message.toString());
            refreshAll();
        }

    }

     
    protected void refreshAll(){
	StringBuilder message = new StringBuilder();
	try{
	    settings = mTransformJPanel.getTransformContext().transform().getSettings();
	    message.append("Refreshing: "+ mTransformJPanel.getTransformContext().getTransformDesc().getDisplayName() );
	    for( Map.Entry<String, Refreshable> refreshableMapEntry : refreshableMap.entrySet() ){
		String componentName = refreshableMapEntry.getKey();
		Refreshable refreshableComponent = refreshableMapEntry.getValue();
		refreshableComponent.doRefresh(settings);
		message.append("\n  " + componentName);
	    }
	}
	catch(Exception e){
	    try{
		Util.handleExceptionWithRestart("ERROR REFRESHING: " + mTransformJPanel.getTransformContext().getMackageDesc().getDisplayName(), e);
	    }
	    catch(Exception f){
		Util.handleExceptionNoRestart("ERROR REFRESHING: " + mTransformJPanel.getTransformContext().getMackageDesc().getDisplayName(), f);
		new RefreshFailureDialog( mTransformJPanel.getTransformContext().getTransformDesc().getDisplayName() );
	    }
	}
	finally{
	    Util.printMessage(message.toString());
	}
	
    }
    

    public void collapseControlPanel(){
	if( expandJDialog.isVisible() )
	    expandJDialog.setVisible(false);
    }
    

    public JButton saveJButton(){ return saveJButton; }
    public JButton reloadJButton(){ return reloadJButton; }
    public JButton removeJButton(){ return removeJButton; }
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        stateButtonGroup = new javax.swing.ButtonGroup();
        socketJPanel = new javax.swing.JPanel();
        contentJPanel = new javax.swing.JPanel();
        mTabbedPane = new javax.swing.JTabbedPane();
        removeJButton = new javax.swing.JButton();
        expandJButton = new javax.swing.JButton();
        reloadJButton = new javax.swing.JButton();
        saveJButton = new javax.swing.JButton();
        readoutJLabel = new javax.swing.JLabel();
        backgroundJLabel = new com.metavize.gui.widgets.MTiledIconLabel();

        setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

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

        removeJButton.setFont(new java.awt.Font("Arial", 0, 12));
        removeJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/metavize/gui/images/Button_Remove_Appliance_106x17.png")));
        removeJButton.setDoubleBuffered(true);
        removeJButton.setFocusPainted(false);
        removeJButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        removeJButton.setIconTextGap(0);
        removeJButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
        removeJButton.setMaximumSize(new java.awt.Dimension(130, 25));
        removeJButton.setMinimumSize(new java.awt.Dimension(130, 25));
        removeJButton.setPreferredSize(new java.awt.Dimension(130, 25));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        contentJPanel.add(removeJButton, gridBagConstraints);

        expandJButton.setFont(new java.awt.Font("Arial", 0, 12));
        expandJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/metavize/gui/images/Button_Expand_Settings_106x17.png")));
        expandJButton.setDoubleBuffered(true);
        expandJButton.setFocusPainted(false);
        expandJButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        expandJButton.setIconTextGap(0);
        expandJButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
        expandJButton.setMaximumSize(new java.awt.Dimension(140, 25));
        expandJButton.setMinimumSize(new java.awt.Dimension(140, 25));
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
        reloadJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/metavize/gui/images/Button_Reload_Settings_106x17.png")));
        reloadJButton.setDoubleBuffered(true);
        reloadJButton.setFocusPainted(false);
        reloadJButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        reloadJButton.setIconTextGap(0);
        reloadJButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
        reloadJButton.setMaximumSize(new java.awt.Dimension(115, 25));
        reloadJButton.setMinimumSize(new java.awt.Dimension(115, 25));
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
        saveJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/metavize/gui/images/Button_Save_Settings_106x17.png")));
        saveJButton.setDoubleBuffered(true);
        saveJButton.setFocusPainted(false);
        saveJButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        saveJButton.setIconTextGap(0);
        saveJButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
        saveJButton.setMaximumSize(new java.awt.Dimension(115, 25));
        saveJButton.setMinimumSize(new java.awt.Dimension(115, 25));
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

        add(socketJPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(13, 7, 570, 354));

        readoutJLabel.setFont(new java.awt.Font("Default", 0, 12));
        readoutJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        readoutJLabel.setText("Settings expanded...");
        readoutJLabel.setBorder(new javax.swing.border.EtchedBorder());
        readoutJLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        readoutJLabel.setIconTextGap(0);
        add(readoutJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(175, 50, 250, 250));

        backgroundJLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/metavize/gui/images/LightGreyBackground1600x100.png")));
        backgroundJLabel.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(128, 127, 127)));
        backgroundJLabel.setDoubleBuffered(true);
        add(backgroundJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 596, 380));

    }//GEN-END:initComponents

    private void saveJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveJButtonActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_saveJButtonActionPerformed

    private void reloadJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reloadJButtonActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_reloadJButtonActionPerformed

    private void expandJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_expandJButtonActionPerformed
	if( !MTransformControlsJPanel.this.expandJDialog.isVisible() ){
	    // change layout
            MTransformControlsJPanel.this.socketJPanel.remove(contentJPanel);
            MTransformControlsJPanel.this.socketJPanel.revalidate();
            MTransformControlsJPanel.this.expandJDialog.getContentPane().add(contentJPanel, contentConstraints, 0);

            // place new window in the center of parent window and show
            MTransformControlsJPanel.this.expandJDialog.setBounds( Util.generateCenteredBounds(Util.getMMainJFrame().getBounds(),
											       Util.getMMainJFrame().getWidth()-EXPAND_INSET,
											       Util.getMMainJFrame().getHeight()-EXPAND_INSET) );
	    expandJButton.setIcon(Util.getButtonCollapseSettings());
            MTransformControlsJPanel.this.expandJDialog.setVisible(true);

            // cleanup after new window is closed
	    expandJButton.setIcon(Util.getButtonExpandSettings());
            MTransformControlsJPanel.this.expandJDialog.getContentPane().remove(contentJPanel);
            MTransformControlsJPanel.this.socketJPanel.add(contentJPanel);
            MTransformControlsJPanel.this.socketJPanel.revalidate();
	}
	else{
	    MTransformControlsJPanel.this.expandJDialog.setVisible(false);
	}
    }//GEN-LAST:event_expandJButtonActionPerformed
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel backgroundJLabel;
    protected javax.swing.JPanel contentJPanel;
    protected javax.swing.JButton expandJButton;
    protected javax.swing.JTabbedPane mTabbedPane;
    protected javax.swing.JLabel readoutJLabel;
    protected javax.swing.JButton reloadJButton;
    protected javax.swing.JButton removeJButton;
    protected javax.swing.JButton saveJButton;
    protected javax.swing.JPanel socketJPanel;
    private javax.swing.ButtonGroup stateButtonGroup;
    // End of variables declaration//GEN-END:variables



}





  
