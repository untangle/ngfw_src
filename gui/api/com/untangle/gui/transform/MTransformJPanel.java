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

import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.Constructor;
import java.util.*;
import javax.swing.*;

import com.untangle.gui.util.*;
import com.untangle.gui.widgets.*;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.mvvm.*;
import com.untangle.mvvm.policy.*;
import com.untangle.mvvm.security.*;
import com.untangle.mvvm.toolbox.MackageDesc;
import com.untangle.mvvm.tran.*;

public class MTransformJPanel extends javax.swing.JPanel {

    // MVVM MODEL
    protected TransformContext transformContext;
    protected Transform transform;
    public Transform getTransform(){ return transform; }
    protected TransformDesc transformDesc;
    public TransformDesc getTransformDesc(){ return transformDesc; }
    protected MackageDesc mackageDesc;
    public MackageDesc getMackageDesc(){ return mackageDesc; }
    public MackageDesc getNewMackageDesc() throws Exception{ return transformContext.getMackageDesc(); }
    protected Tid tid;
    public Tid getTid(){ return tid; }
    protected Policy policy;
    public Policy getPolicy(){ return policy; }
    private void setPolicy(Policy policy){ this.policy = policy; }

    // GUI VIEW MODEL
    private MTransformControlsJPanel mTransformControlsJPanel;
    protected MTransformControlsJPanel mTransformControlsJPanel(){ return mTransformControlsJPanel; }
    private MTransformDisplayJPanel mTransformDisplayJPanel;
    public MTransformDisplayJPanel mTransformDisplayJPanel() { return mTransformDisplayJPanel; }

    // GUI DATA MODEL
    protected MStateMachine mStateMachine;
    CycleJLabel powerOnHintJLabel;
    private static ImageIcon[] powerOnImageIcons;
    private DropdownTask controlsDropdownTask;
    private volatile boolean controlsLoaded;
    private volatile boolean showingSettings;
    private ShowControlsThread showControlsThread;

    // GUI CONSTANTS
    private static Dimension maxDimension, minDimension;
    private static final int HELPER_POWER_ON_BLINK = 200;

    // SHUTDOWNABLE
    private Map<String, Shutdownable> shutdownableMap = new LinkedHashMap(1);
    protected void addShutdownable(String name, Shutdownable shutdownable){ shutdownableMap.put(name, shutdownable); }
    protected void removeShutdownable(String shutdownableKey){ shutdownableMap.remove(shutdownableKey); }

    public static MTransformJPanel instantiate(TransformContext transformContext, TransformDesc transformDesc, Policy policy) throws Exception {
        Class guiClass = Util.getClassLoader().loadClass( transformDesc.getGuiClassName(), transformDesc );
        Constructor guiConstructor = guiClass.getConstructor( new Class[]{TransformContext.class, TransformDesc.class} );
        MTransformJPanel mTransformJPanel = (MTransformJPanel) guiConstructor.newInstance(new Object[]{transformContext, transformDesc});
	mTransformJPanel.setPolicy(policy);
        return mTransformJPanel;
    }

    public MTransformJPanel(TransformContext transformContext, TransformDesc transformDesc) { // this should not be instantiated
        setDoubleBuffered(true);
        this.transformContext = transformContext;
	this.transformDesc = transformDesc;
	transform = transformContext.transform();
	mackageDesc = transformContext.getMackageDesc();
	tid = transformDesc.getTid();
        controlsLoaded = false;
        showControlsThread = new ShowControlsThread();

        // VISUAL HELPER
        synchronized( this ){
            if( powerOnImageIcons == null ){
                String[] powerOnImagePaths = { "com/untangle/gui/transform/IconPowerOnHint30.png",
                                               "com/untangle/gui/transform/IconPowerOnHint40.png",
                                               "com/untangle/gui/transform/IconPowerOnHint50.png",
                                               "com/untangle/gui/transform/IconPowerOnHint60.png",
                                               "com/untangle/gui/transform/IconPowerOnHint70.png",
                                               "com/untangle/gui/transform/IconPowerOnHint80.png",
                                               "com/untangle/gui/transform/IconPowerOnHint90.png",
                                               "com/untangle/gui/transform/IconPowerOnHint100.png" };
                powerOnImageIcons = Util.getImageIcons( powerOnImagePaths );
            }
        }
        powerOnHintJLabel = new CycleJLabel(powerOnImageIcons, HELPER_POWER_ON_BLINK, true, true);
        setPowerOnHintVisible(transform.neverStarted());

        // INIT GUI
        initComponents();
        jProgressBar.setVisible(false);

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
            // SHOW A LITTLE MESSAGE TELLING THEM TO RESTART
            mTransformControlsJPanel = new MTransformControlsJPanel(this){public void generateGui(){}};
            JPanel warningJPanel = new JPanel(new BorderLayout());
            JLabel warningJLabel = new JLabel("<html><center><b>Warning:</b> Settings could not be loaded properly." +
                                              "<br>Please restart the Untangle Client to load settings properly.</center></html>");
            warningJLabel.setFont(new java.awt.Font("Arial", 0, 14));
            warningJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            warningJPanel.add(warningJLabel);
            mTransformControlsJPanel.getMTabbedPane().add("Warning", warningJPanel);
            Util.handleExceptionNoRestart("Error adding control panel", e);
        }

        // DYNAMICALLY LOAD ICONS
	String name = null;
        try{
            name = transformDesc.getName();
            name = name.substring(0, name.indexOf('-'));
            descriptionIconJLabel.setIcon(new javax.swing.ImageIcon( Util.getClassLoader().getResource("com/untangle/tran/"
                                                                                                       + name
                                                                                                       +  "/gui/IconDesc42x42.png")));
        }
        catch(Exception e){ Util.handleExceptionNoRestart("Error adding icon: " + name , e); }
        
               organizationIconJLabel.setIcon(null);
               /*
        try{
            name = transformDesc.getName();
            name = name.substring(0, name.indexOf('-'));
            organizationIconJLabel.setIcon(new javax.swing.ImageIcon( Util.getClassLoader().getResource("com/untangle/tran/"
                                                                                                        + name
                                                                                                        + "/gui/IconOrg42x42.png")));
        }
        catch(Exception e){ Util.handleExceptionNoRestart("Error adding icon: " + name , e); }
        
               */
        // SIZES
        if(maxDimension == null)
            maxDimension = new Dimension((int)this.getPreferredSize().getWidth(), (int)(this.getPreferredSize().getHeight()
                                                                                        + mTransformControlsJPanel.getPreferredSize().getHeight()));
        if(minDimension == null)
            minDimension = new Dimension((int)this.getPreferredSize().getWidth(), (int)(this.getPreferredSize().getHeight()));
        setPreferredSize(minDimension);
        setMinimumSize(minDimension);
        setMaximumSize(minDimension);

        // ADD CONFIG PANEL
        add(mTransformControlsJPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(46, 100, 596, 380));
        setMinimumSize(minDimension);
        setMaximumSize(maxDimension);
        //mTransformControlsJPanel.setVisible(false);
        setPreferredSize(minDimension);

        // CONFIG PANEL DROPDOWN TASK /////////
        controlsDropdownTask = new DropdownTask(this, mTransformControlsJPanel, controlsJToggleButton,
                                                minDimension, maxDimension,
                                                596, 380, 46, -280, 100);

	// SHUTDOWNABLE //
	addShutdownable("ShowControlsThread", showControlsThread);

        // SETUP NAME AND MESSAGE
	try{ ((JComponent)descriptionTextJLabel).putClientProperty(com.sun.java.swing.SwingUtilities2.AA_TEXT_PROPERTY_KEY, new Boolean(true)); }
	catch(Throwable t){}
	descriptionTextJLabel.setText( transformDesc.getDisplayName() );
	try{ ((JComponent)messageTextJLabel).putClientProperty(com.sun.java.swing.SwingUtilities2.AA_TEXT_PROPERTY_KEY, new Boolean(true)); }
	catch(Throwable t){}
	String extraName = mackageDesc.getExtraName();
	if( extraName != null )
	    messageTextJLabel.setText( extraName );
	else
	    messageTextJLabel.setText("");

        // SETUP STATE
        mStateMachine = new MStateMachine(this);
        powerJToggleButton.addActionListener(mStateMachine);
        mTransformControlsJPanel.saveJButton().addActionListener(mStateMachine);
        mTransformControlsJPanel.reloadJButton().addActionListener(mStateMachine);
        mTransformControlsJPanel.removeJButton().addActionListener(mStateMachine);
    }

    public void highlight(){ new FadeTask(effectsJPanel,true); }

    public void setProblemView(boolean doLater){ mStateMachine.setProblemView(doLater); }
    public void setRemovingView(boolean doLater){ mStateMachine.setRemovingView(doLater); }

    public void setPowerOnHintVisible(boolean isVisible){
        if( isVisible )
            powerOnHintJLabel.start();
        else{
            powerOnHintJLabel.stop();
        }
    }

    public void doShutdown(){
	for( Map.Entry<String,Shutdownable> shutdownableEntry : shutdownableMap.entrySet()){
	    shutdownableEntry.getValue().doShutdown();
	}
	SwingUtilities.invokeLater( new Runnable(){ public void run(){
	    mTransformControlsJPanel.doShutdown();
	    mTransformDisplayJPanel.doShutdown();
	    setControlsShowing(false);
	}});
    }

    public void doRefreshState(){
	mStateMachine.doRefreshState();
    }


    public JToggleButton powerJToggleButton(){ return powerJToggleButton; }
    BlinkJLabel stateJLabel(){ return (BlinkJLabel) stateJLabel; }
    JLabel messageTextJLabel(){ return messageTextJLabel; }
        private void initComponents() {//GEN-BEGIN:initComponents
                onOffbuttonGroup = new javax.swing.ButtonGroup();
                descriptionTextJLabel = new javax.swing.JLabel();
                nbPowerOnHintJLabel = powerOnHintJLabel;
                stateJLabel = (JLabel) new com.untangle.gui.transform.BlinkJLabel();
                controlsJToggleButton = new javax.swing.JToggleButton();
                descriptionIconJLabel = new javax.swing.JLabel();
                organizationIconJLabel = new javax.swing.JLabel();
                jProgressBar = new javax.swing.JProgressBar();
                messageTextJLabel = new javax.swing.JLabel();
                powerJToggleButton = new javax.swing.JToggleButton();
                effectsJPanel = new javax.swing.JPanel();
                backgroundJLabel = new javax.swing.JLabel();

                setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

                setMaximumSize(new java.awt.Dimension(688, 500));
                setMinimumSize(new java.awt.Dimension(688, 100));
                setOpaque(false);
                setPreferredSize(new java.awt.Dimension(688, 100));
                descriptionTextJLabel.setFont(new java.awt.Font("Arial", 0, 18));
                descriptionTextJLabel.setForeground(new java.awt.Color(124, 123, 123));
                descriptionTextJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                descriptionTextJLabel.setText("SuperTransform");
                descriptionTextJLabel.setDoubleBuffered(true);
                descriptionTextJLabel.setFocusable(false);
                descriptionTextJLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
                descriptionTextJLabel.setIconTextGap(0);
                add(descriptionTextJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(106, 16, -1, 20));

                nbPowerOnHintJLabel.setFont(new java.awt.Font("Arial", 0, 18));
                nbPowerOnHintJLabel.setForeground(new java.awt.Color(255, 0, 0));
                nbPowerOnHintJLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
                nbPowerOnHintJLabel.setDoubleBuffered(true);
                nbPowerOnHintJLabel.setFocusable(false);
                nbPowerOnHintJLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
                nbPowerOnHintJLabel.setIconTextGap(0);
                add(nbPowerOnHintJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(270, 80, -1, -1));

                stateJLabel.setToolTipText("<HTML> The <B>Status Indicator</B> shows the current operating condition of a particular software product.<BR>\n<font color=\"00FF00\"><b>Green</b></font> indicates that the product is \"on\" and operating normally.<BR>\n<font color=\"FF0000\"><b>Red</b></font> indicates that the product is \"on\", but that an abnormal condition has occurred.<BR>\n<font color=\"FFFF00\"><b>Yellow</b></font> indicates that the product is saving or refreshing settings.<BR>\n<b>Clear</b> indicates that the product is \"off\", and may be turned \"on\" by the user.\n</HTML>");
                add(stateJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(606, 20, 28, 28));

                controlsJToggleButton.setFont(new java.awt.Font("Default", 0, 12));
                controlsJToggleButton.setIcon(new javax.swing.ImageIcon( Util.getClassLoader().getResource("com/untangle/gui/transform/IconControlsClosed28x28.png")));
                controlsJToggleButton.setText("Show Settings");
                controlsJToggleButton.setAlignmentX(0.5F);
                controlsJToggleButton.setDoubleBuffered(true);
                controlsJToggleButton.setFocusPainted(false);
                controlsJToggleButton.setFocusable(false);
                controlsJToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
                controlsJToggleButton.setIconTextGap(0);
                controlsJToggleButton.setMargin(new java.awt.Insets(0, 0, 1, 3));
                controlsJToggleButton.setOpaque(false);
                controlsJToggleButton.setSelectedIcon(new javax.swing.ImageIcon( Util.getClassLoader().getResource("com/untangle/gui/transform/IconControlsOpen28x28.png")));
                controlsJToggleButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                controlsJToggleButtonActionPerformed(evt);
                        }
                });

                add(controlsJToggleButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(52, 60, 120, 25));

                descriptionIconJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                descriptionIconJLabel.setIcon(new javax.swing.ImageIcon( Util.getClassLoader().getResource("com/untangle/gui/transform/IconDesc42x42.png")));
                descriptionIconJLabel.setDoubleBuffered(true);
                descriptionIconJLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
                add(descriptionIconJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(52, 6, 42, 42));

                organizationIconJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                organizationIconJLabel.setIcon(new javax.swing.ImageIcon( Util.getClassLoader().getResource("com/untangle/gui/transform/IconOrg42x42.png")));
                organizationIconJLabel.setAlignmentX(0.5F);
                organizationIconJLabel.setDoubleBuffered(true);
                organizationIconJLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
                organizationIconJLabel.setIconTextGap(0);
                add(organizationIconJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(52, 51, 42, 42));

                jProgressBar.setFont(new java.awt.Font("Dialog", 0, 12));
                jProgressBar.setMaximumSize(new java.awt.Dimension(232, 20));
                jProgressBar.setMinimumSize(new java.awt.Dimension(232, 20));
                jProgressBar.setPreferredSize(new java.awt.Dimension(232, 20));
                jProgressBar.setString("");
                jProgressBar.setStringPainted(true);
                add(jProgressBar, new org.netbeans.lib.awtextra.AbsoluteConstraints(106, 37, -1, -1));

                messageTextJLabel.setFont(new java.awt.Font("Arial", 1, 12));
                messageTextJLabel.setForeground(new java.awt.Color(68, 91, 255));
                messageTextJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                messageTextJLabel.setText("SuperTransform");
                messageTextJLabel.setDoubleBuffered(true);
                messageTextJLabel.setFocusable(false);
                messageTextJLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
                messageTextJLabel.setIconTextGap(0);
                add(messageTextJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(106, 37, -1, 20));

                powerJToggleButton.setFont(new java.awt.Font("Default", 0, 12));
                powerJToggleButton.setIcon(new javax.swing.ImageIcon( Util.getClassLoader().getResource("com/untangle/gui/transform/IconPowerOffState28x28.png")));
                powerJToggleButton.setToolTipText("<HTML>\nThe <B>Power Button</B> allows you to turn a product \"on\" and \"off\".<br>\n\n</HTML>");
                powerJToggleButton.setAlignmentX(0.5F);
                powerJToggleButton.setBorderPainted(false);
                powerJToggleButton.setContentAreaFilled(false);
                powerJToggleButton.setDoubleBuffered(true);
                powerJToggleButton.setFocusPainted(false);
                powerJToggleButton.setFocusable(false);
                powerJToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
                powerJToggleButton.setIconTextGap(0);
                powerJToggleButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
                powerJToggleButton.setMaximumSize(new java.awt.Dimension(28, 28));
                powerJToggleButton.setMinimumSize(new java.awt.Dimension(28, 28));
                powerJToggleButton.setPreferredSize(new java.awt.Dimension(28, 28));
                powerJToggleButton.setSelectedIcon(new javax.swing.ImageIcon( Util.getClassLoader().getResource("com/untangle/gui/transform/IconPowerOnState28x28.png")));
                add(powerJToggleButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(606, 54, 28, 28));

                effectsJPanel.setBackground(new Color(255,255,255,0));
                add(effectsJPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 688, 100));

                backgroundJLabel.setIcon(new javax.swing.ImageIcon( Util.getClassLoader().getResource("com/untangle/gui/transform/TransformBackground688x100.png")));
                backgroundJLabel.setDisabledIcon(new javax.swing.ImageIcon(""));
                backgroundJLabel.setDoubleBuffered(true);
                backgroundJLabel.setOpaque(true);
                add(backgroundJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 688, 100));

        }//GEN-END:initComponents

    private void controlsJToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_controlsJToggleButtonActionPerformed
        handleControlsJButton(controlsJToggleButton.isSelected());
    }//GEN-LAST:event_controlsJToggleButtonActionPerformed


    // SHOW/HIDE CONTROLS ////////////
    public void setControlsShowing(boolean showingBoolean){
        handleControlsJButton(showingBoolean);
    }
    public boolean getControlsShowing(){ return controlsJToggleButton.isSelected(); }
    public JToggleButton getControlsJToggleButton(){ return controlsJToggleButton; };

    private void handleControlsJButton(boolean showSettings){
        showingSettings = showSettings;
        controlsJToggleButton.setEnabled(false);
        synchronized(showControlsThread){
            showControlsThread.notify();
        }
    }


    private Exception generateGuiException;
    private class ShowControlsThread extends Thread implements Shutdownable {
	private volatile boolean stop = false;
        public ShowControlsThread(){
	    super("MVCLIENT-ShowControlsThread: " + MTransformJPanel.this.transformDesc.getDisplayName());
            setDaemon(true);
            start();
        }
	public synchronized void doShutdown(){
		stop = true;
		notify();
	}
        public void run(){
            try{
                while(true){
                    synchronized(this){
			if(stop)
			    break;
                        wait();
			if(stop)
			    break;
                        if(MTransformJPanel.this.showingSettings && !MTransformJPanel.this.controlsLoaded){
                            SwingUtilities.invokeLater( new Runnable(){ public void run(){
                                jProgressBar.setVisible(true);
                                jProgressBar.setIndeterminate(true);
                                jProgressBar.setString("Loading Settings...");
                            }});
			    try{
				// LOAD SETTINGS //
				mTransformControlsJPanel.refreshAll();
				// GENERATE GUI //
				generateGuiException = null;
				SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
				    try{ mTransformControlsJPanel.generateGui(); }
				    catch(Exception f){ generateGuiException = f; }
				}});
				if( generateGuiException != null )
				    throw generateGuiException;
				// POPULATE GUI //
				mTransformControlsJPanel.populateAll();
			    }
			    catch(Exception e){
				try{ Util.handleExceptionWithRestart("Error showing settings", e); }
				catch(Exception f){
				    Util.handleExceptionNoRestart("Error showing settings", f);
				    RefreshFailureDialog.factory( (Window) mTransformControlsJPanel.getContentJPanel().getTopLevelAncestor(),
								  transformDesc.getDisplayName());
				}
			    }
                            MTransformJPanel.this.controlsLoaded = true;
                            SwingUtilities.invokeLater( new Runnable(){ public void run(){
                                jProgressBar.setIndeterminate(false);
                                jProgressBar.setString("Settings Loaded");
                                jProgressBar.setValue(100);
                            }});
                        }
                        SwingUtilities.invokeLater( new Runnable(){ public void run(){
                            MTransformJPanel.this.controlsDropdownTask.start(MTransformJPanel.this.showingSettings, jProgressBar);
                        }});
                    }
                }
            }
            catch(Exception e){
                Util.handleExceptionNoRestart("Error waiting", e);
            }
        }
    }

    public void focus(){
        Rectangle newBounds = this.getBounds();
        newBounds.width = this.getPreferredSize().width;
        newBounds.height = this.getPreferredSize().height;
        //Util.getMPipelineJPanel().focusMTransformJPanel(newBounds);
    }
    //////////////////////////////



        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JLabel backgroundJLabel;
        protected javax.swing.JToggleButton controlsJToggleButton;
        protected javax.swing.JLabel descriptionIconJLabel;
        protected javax.swing.JLabel descriptionTextJLabel;
        private javax.swing.JPanel effectsJPanel;
        private javax.swing.JProgressBar jProgressBar;
        protected javax.swing.JLabel messageTextJLabel;
        protected javax.swing.JLabel nbPowerOnHintJLabel;
        private javax.swing.ButtonGroup onOffbuttonGroup;
        protected javax.swing.JLabel organizationIconJLabel;
        protected javax.swing.JToggleButton powerJToggleButton;
        private javax.swing.JLabel stateJLabel;
        // End of variables declaration//GEN-END:variables

}
