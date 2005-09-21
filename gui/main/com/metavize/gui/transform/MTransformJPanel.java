
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
import com.metavize.gui.util.*;
import com.metavize.gui.util.*;

import com.metavize.mvvm.security.*;
import com.metavize.mvvm.tran.*;
import com.metavize.mvvm.*;

import java.awt.*;
import java.lang.reflect.Constructor;
import java.util.*;
import javax.swing.*;


public class MTransformJPanel extends javax.swing.JPanel {

    protected TransformContext transformContext;
    public TransformContext transformContext() { return transformContext; }
    public TransformContext getTransformContext(){ return transformContext; }
    protected MackageDesc mackageDesc;
    public MackageDesc mackageDesc(){ return mackageDesc; }
    public MackageDesc getMackageDesc(){ return mackageDesc; }
    private MTransformControlsJPanel mTransformControlsJPanel; protected MTransformControlsJPanel mTransformControlsJPanel(){ return mTransformControlsJPanel; }
    private MTransformDisplayJPanel mTransformDisplayJPanel;  protected MTransformDisplayJPanel mTransformDisplayJPanel() { return mTransformDisplayJPanel; }


    protected MStateMachine mStateMachine;

    private Tid tid;
    public Tid getTid(){ return tid; }


    // GUI assets
    private static Dimension maxDimension, minDimension;

    public static MTransformJPanel instantiate(TransformContext transformContext) throws Exception {
	TransformDesc transformDesc = transformContext.getTransformDesc();
	Class guiClass = Util.getClassLoader().loadClass( transformDesc.getGuiClassName(), transformDesc.getName() );
	Constructor guiConstructor = guiClass.getConstructor( new Class[]{TransformContext.class} );
	MTransformJPanel mTransformJPanel = (MTransformJPanel) guiConstructor.newInstance(new Object[]{transformContext});
	return mTransformJPanel;
    }
    
    public MTransformJPanel(TransformContext transformContext) {
        this.transformContext = transformContext;
	this.mackageDesc = transformContext.getMackageDesc();
	this.tid = transformContext.getTid();
	
        // INIT GUI
        initComponents();
	
        // DYNAMICALLY LOAD DISPLAY
        try{
            Class mTransformDisplayJPanelClass = Class.forName(this.getClass().getPackage().getName()  +  ".MTransformDisplayJPanel",
							       true, Util.getClassLoader() );
            Constructor mTransformDisplayJPanelConstructor = mTransformDisplayJPanelClass.getConstructor(new Class[]{this.getClass()});
            mTransformDisplayJPanel = (MTransformDisplayJPanel) mTransformDisplayJPanelConstructor.newInstance(new Object[]{this});
        }
        catch(Exception e){
            mTransformDisplayJPanel = new MTransformDisplayJPanel(this);
            Util.handleExceptionNoRestart("Error adding display panel", e);
        }
        this.add(mTransformDisplayJPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(104, 5, 498, 90), 0);
	
        // DYNAMICALLY LOAD CONFIG
        try{
            Class mTransformControlsJPanelClass = Class.forName(this.getClass().getPackage().getName()  +  ".MTransformControlsJPanel",
								true, Util.getClassLoader() );
            Constructor mTransformControlsJPanelConstructor = mTransformControlsJPanelClass.getConstructor(new Class[]{this.getClass()});
            mTransformControlsJPanel = (MTransformControlsJPanel) mTransformControlsJPanelConstructor.newInstance(new Object[]{this});
        }
        catch(Exception e){
            mTransformControlsJPanel = new MTransformControlsJPanel(this);
            Util.handleExceptionNoRestart("Error adding control panel", e);
        }
        
	
	
	// DYNAMICALLY LOAD ICONS
	String name = null;
	try{
	    name = getMackageDesc().getName();
	    name = name.substring(0, name.indexOf('-'));
	    
	    descriptionIconJLabel.setIcon(new javax.swing.ImageIcon( Util.getClassLoader().getResource("com/metavize/tran/" + name +  "/gui/IconDesc42x42.png")));
	    organizationIconJLabel.setIcon(new javax.swing.ImageIcon( Util.getClassLoader().getResource("com/metavize/tran/" + name + "/gui/IconOrg42x42.png")));
	}
	catch(Exception e){
	    Util.handleExceptionNoRestart("Error adding icons: " + name , e);
	}
	
	
	// SIZES
	if(maxDimension == null)
            maxDimension = new Dimension((int)this.getPreferredSize().getWidth(), (int)(this.getPreferredSize().getHeight() + mTransformControlsJPanel.getPreferredSize().getHeight()));
        if(minDimension == null)
            minDimension = new Dimension((int)this.getPreferredSize().getWidth(), (int)(this.getPreferredSize().getHeight()));
        setPreferredSize(minDimension);
        setMinimumSize(minDimension);
        setMaximumSize(minDimension);
	
        // ADD CONFIG PANEL
        add(mTransformControlsJPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(46, 100, 596, 380), 0);
        setMinimumSize(minDimension);
        setMaximumSize(maxDimension);
        //mTransformControlsJPanel.setVisible(false);
        setPreferredSize(minDimension);
        
        
        // SETUP COLORS and name
        descriptionTextJLabel.setText( getMackageDesc().getDisplayName() );
	
	
        // SETUP STATE
        mStateMachine = new MStateMachine(this);
        powerJToggleButton.addActionListener(mStateMachine);
        mTransformControlsJPanel.saveJButton().addActionListener(mStateMachine);
        mTransformControlsJPanel.reloadJButton().addActionListener(mStateMachine);
        mTransformControlsJPanel.removeJButton().addActionListener(mStateMachine);
	
    }
    
    public void doShutdown(){
	try{
	    SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
		mTransformControlsJPanel.doShutdown();
		mTransformDisplayJPanel.doShutdown();
		setControlsShowing(false);
	    }});
	}
	catch(Exception e){
	    Util.handleExceptionNoRestart("Error doing shutdown", e);
	}
    }

    JToggleButton powerJToggleButton(){ return powerJToggleButton; }
    BlinkJLabel stateJLabel(){ return (BlinkJLabel) stateJLabel; }

    private void initComponents() {//GEN-BEGIN:initComponents
        onOffbuttonGroup = new javax.swing.ButtonGroup();
        descriptionTextJLabel = new javax.swing.JLabel();
        stateJLabel = (JLabel) new com.metavize.gui.transform.BlinkJLabel();
        controlsJToggleButton = new javax.swing.JToggleButton();
        descriptionIconJLabel = new javax.swing.JLabel();
        organizationIconJLabel = new javax.swing.JLabel();
        powerJToggleButton = new javax.swing.JToggleButton();
        backgroundJLabel = new javax.swing.JLabel();

        setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        setMaximumSize(new java.awt.Dimension(688, 500));
        setMinimumSize(new java.awt.Dimension(688, 100));
        setPreferredSize(new java.awt.Dimension(688, 100));
        setOpaque(false);
        descriptionTextJLabel.setFont(new java.awt.Font("Arial", 0, 18));
        descriptionTextJLabel.setForeground(new java.awt.Color(124, 123, 123));
        descriptionTextJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        descriptionTextJLabel.setText("SuperTransform");
        descriptionTextJLabel.setDoubleBuffered(true);
        descriptionTextJLabel.setFocusable(false);
        descriptionTextJLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        descriptionTextJLabel.setIconTextGap(0);
        add(descriptionTextJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(106, 16, -1, 20));

        stateJLabel.setToolTipText("<HTML> The <B>Status Indicator</B> shows the current operating condition of a particular software appliance.<BR>\n<font color=\"00FF00\"><b>Green</b></font> indicates that the appliance is \"on\" and operating normally.<BR>\n<font color=\"FF0000\"><b>Red</b></font> indicates that the appliance is \"on\", but that an abnormal condition has occurred.<BR>\n<font color=\"FFFF00\"><b>Yellow</b></font> indicates that the appliance is saving or refreshing settings.<BR>\n<b>Clear</b> indicates that the appliance is \"off\", and may be turned \"on\" by the user.\n</HTML>");
        add(stateJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(606, 20, 28, 28));

        controlsJToggleButton.setFont(new java.awt.Font("Default", 0, 12));
        controlsJToggleButton.setIcon(new javax.swing.ImageIcon( Util.getClassLoader().getResource("com/metavize/gui/transform/IconControlsClosed28x28.png")));
        controlsJToggleButton.setText("Show Settings");
        controlsJToggleButton.setToolTipText("<HTML>\nThe <B>Settings Button</B> allows you to show and hide configuration for<BR>\na particular software appliance.  Press the Settings Button once to show the<BR>\nsettings, and press the button again to hide the settings.\n</HTML>");
        controlsJToggleButton.setAlignmentX(0.5F);
        controlsJToggleButton.setDoubleBuffered(true);
        controlsJToggleButton.setFocusPainted(false);
        controlsJToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        controlsJToggleButton.setIconTextGap(0);
        controlsJToggleButton.setMargin(new java.awt.Insets(0, 0, 1, 3));
        controlsJToggleButton.setSelectedIcon(new javax.swing.ImageIcon( Util.getClassLoader().getResource("com/metavize/gui/transform/IconControlsOpen28x28.png")));
        controlsJToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                controlsJToggleButtonActionPerformed(evt);
            }
        });

        add(controlsJToggleButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(106, 60, 120, 25));

        descriptionIconJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        descriptionIconJLabel.setIcon(new javax.swing.ImageIcon( Util.getClassLoader().getResource("com/metavize/gui/transform/IconDesc42x42.png")));
        descriptionIconJLabel.setDoubleBuffered(true);
        descriptionIconJLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        add(descriptionIconJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(52, 6, 42, 42));

        organizationIconJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        organizationIconJLabel.setIcon(new javax.swing.ImageIcon( Util.getClassLoader().getResource("com/metavize/gui/transform/IconOrg42x42.png")));
        organizationIconJLabel.setAlignmentX(0.5F);
        organizationIconJLabel.setDoubleBuffered(true);
        organizationIconJLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        organizationIconJLabel.setIconTextGap(0);
        add(organizationIconJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(52, 51, 42, 42));

        powerJToggleButton.setFont(new java.awt.Font("Default", 0, 12));
        powerJToggleButton.setIcon(new javax.swing.ImageIcon( Util.getClassLoader().getResource("com/metavize/gui/transform/IconPowerOffState28x28.png")));
        powerJToggleButton.setToolTipText("<HTML>\nThe <B>Power Button</B> allows you to turn a particular Software Appliance \"on\" and \"off\".<br>\n\n</HTML>");
        powerJToggleButton.setAlignmentX(0.5F);
        powerJToggleButton.setBorderPainted(false);
        powerJToggleButton.setContentAreaFilled(false);
        powerJToggleButton.setDoubleBuffered(true);
        powerJToggleButton.setFocusPainted(false);
        powerJToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        powerJToggleButton.setIconTextGap(0);
        powerJToggleButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
        powerJToggleButton.setMaximumSize(new java.awt.Dimension(28, 28));
        powerJToggleButton.setMinimumSize(new java.awt.Dimension(28, 28));
        powerJToggleButton.setPreferredSize(new java.awt.Dimension(28, 28));
        powerJToggleButton.setSelectedIcon(new javax.swing.ImageIcon( Util.getClassLoader().getResource("com/metavize/gui/transform/IconPowerOnState28x28.png")));
        add(powerJToggleButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(606, 54, 28, 28));

        backgroundJLabel.setIcon(new javax.swing.ImageIcon( Util.getClassLoader().getResource("com/metavize/gui/transform/TransformBackground688x100.png")));
        backgroundJLabel.setDisabledIcon(new javax.swing.ImageIcon(""));
        backgroundJLabel.setDoubleBuffered(true);
        backgroundJLabel.setOpaque(true);
        add(backgroundJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 688, 100));

    }//GEN-END:initComponents

    private void controlsJToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_controlsJToggleButtonActionPerformed
        handleControlsJButton(controlsJToggleButton.isSelected());
    }//GEN-LAST:event_controlsJToggleButtonActionPerformed



    public void setControlsShowing(boolean showingBoolean){ handleControlsJButton(showingBoolean); }
    public boolean getControlsShowing(){ return controlsJToggleButton.isSelected(); }
    public JToggleButton getControlsJToggleButton(){ return controlsJToggleButton; };

    private void handleControlsJButton(boolean isShowing){
        
        if(isShowing){
            setPreferredSize(maxDimension);
            controlsJToggleButton.setText("Hide Settings");
        }
        else{
            setPreferredSize(minDimension);
            controlsJToggleButton.setText("Show Settings");
        }
        //mTransformControlsJPanel.setVisible(isShowing);
	//        focus();  people no longer want this shizzle

    }

    public void focus(){
        Rectangle newBounds = this.getBounds();
        newBounds.width = this.getPreferredSize().width;
        newBounds.height = this.getPreferredSize().height;
        //Util.getMPipelineJPanel().focusMTransformJPanel(newBounds);
    }




    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel backgroundJLabel;
    protected javax.swing.JToggleButton controlsJToggleButton;
    protected javax.swing.JLabel descriptionIconJLabel;
    protected javax.swing.JLabel descriptionTextJLabel;
    private javax.swing.ButtonGroup onOffbuttonGroup;
    protected javax.swing.JLabel organizationIconJLabel;
    protected javax.swing.JToggleButton powerJToggleButton;
    private javax.swing.JLabel stateJLabel;
    // End of variables declaration//GEN-END:variables

}
