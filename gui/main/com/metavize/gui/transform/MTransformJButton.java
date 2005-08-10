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

import com.metavize.gui.main.*;
import com.metavize.gui.pipeline.*;
import com.metavize.gui.util.*;
import com.metavize.gui.widgets.dialogs.*;
import com.metavize.gui.widgets.MMultilineToolTip;

import com.metavize.mvvm.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.font.*;
import java.text.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 *
 * @author  Ian Nieves
 */
public class MTransformJButton extends JButton {

    // FADED BACKGROUND /////////////
    private static final int FADE_DELAY_MILLIS = 150;
    private static final float FADE_DECREMENT = .1f;
    private static Color BACKGROUND_COLOR = null;
    private int fadeIteration = -1;
    /////////////////////////////////


    private MackageDesc mackageDesc;
    private GridBagConstraints gridBagConstraints;
    private JProgressBar statusJProgressBar;
    private JLabel statusJLabel;
    private JLabel nameJLabel;
    private JLabel organizationIconJLabel;
    private JLabel descriptionIconJLabel;

    private String toolTipString;


    /** Creates a new instance of MTransformJButton */
    public MTransformJButton(MackageDesc mackageDesc) {

        this.mackageDesc = mackageDesc;

        // INITIAL LAYOUT
        this.setLayout(new GridBagLayout());

        // ORG ICON
        organizationIconJLabel = new JLabel();
        if( mackageDesc.getOrgIcon() != null )
            organizationIconJLabel.setIcon( new javax.swing.ImageIcon(mackageDesc.getOrgIcon()) );
        //organizationIconJLabel.setDisabledIcon(this.orgIcon);
        organizationIconJLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        organizationIconJLabel.setFocusable(false);
        organizationIconJLabel.setPreferredSize(new java.awt.Dimension(42, 42));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 4);
        add(organizationIconJLabel, gridBagConstraints);

        // DESC ICON
        descriptionIconJLabel = new JLabel();
        if( mackageDesc.getDescIcon() != null )
            descriptionIconJLabel.setIcon( new javax.swing.ImageIcon(mackageDesc.getDescIcon()) );
        //descriptionIconJLabel.setDisabledIcon(this.descIcon);
        descriptionIconJLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        descriptionIconJLabel.setFocusable(false);
        descriptionIconJLabel.setPreferredSize(new java.awt.Dimension(42, 42));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 2);
        add(descriptionIconJLabel, gridBagConstraints);

        //DISPLAY NAME
        nameJLabel = new JLabel();
        nameJLabel.setText( "<html><b><center>" + Util.wrapString(mackageDesc.getDisplayName(), 20) + "</center></b></html>");
        nameJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        nameJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        nameJLabel.setFocusable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        add(nameJLabel, gridBagConstraints);

        //status progressbar
        statusJProgressBar = new JProgressBar();
        statusJProgressBar.setBorderPainted(false);
        //statusJProgressBar.setVisible(true);
        statusJProgressBar.setVisible(false);
        statusJProgressBar.setStringPainted(false);
        statusJProgressBar.setOpaque(false);
        statusJProgressBar.setIndeterminate(false);
        statusJProgressBar.setValue(0);
        statusJProgressBar.setForeground(new java.awt.Color(68, 91, 255));
        statusJProgressBar.setFont(new java.awt.Font("Dialog", 0, 12));
        statusJProgressBar.setPreferredSize(new java.awt.Dimension(130, 16));
        statusJProgressBar.setMaximumSize(new java.awt.Dimension(130, 16));
        statusJProgressBar.setMinimumSize(new java.awt.Dimension(50, 16));
        //status label
        statusJLabel = new JLabel();
        statusJLabel.setHorizontalAlignment(JLabel.CENTER);
        statusJLabel.setOpaque(false);
        statusJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        statusJLabel.setPreferredSize(new java.awt.Dimension(130, 16));
        statusJLabel.setMaximumSize(new java.awt.Dimension(130, 16));
        statusJLabel.setMinimumSize(new java.awt.Dimension(50, 16));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        add(statusJLabel, gridBagConstraints);
        add(statusJProgressBar, gridBagConstraints);

        // TOOLTIP
        toolTipString = Util.wrapString( mackageDesc.getLongDescription(), 80);

        this.setMargin(new Insets(0,0,0,0));
        this.setFocusPainted(false);
        this.setContentAreaFilled(true);
        this.setOpaque(true);

	if(BACKGROUND_COLOR == null)
	    BACKGROUND_COLOR = this.getBackground();
    }

    public MTransformJButton duplicate(){
        return new  MTransformJButton( mackageDesc );
    }


    // CONVENIENCE WRAPPERS FOR MACKAGE /////////
    public String getFullDescription(){ return new String( mackageDesc.getLongDescription() ); }
    public String getShortDescription(){ return new String( mackageDesc.getShortDescription() ); }
    public String getName(){ return mackageDesc.getName(); }
    public String getDisplayName(){ return mackageDesc.getDisplayName(); }
    public int    getRackPosition(){ return mackageDesc.getRackPosition(); }
    public String getPrice(){ return mackageDesc.getPrice(); }
    public String getWebpage(){ return mackageDesc.getWebsite(); }
    ////////////////////////////////////////////

    public void highlight(){
	new FadeTask();
    }


    // VIEW UPDATING ///////////
    private void updateView(final String message, final String toolTip, final boolean isEnabled){
	SwingUtilities.invokeLater( new Runnable() { public void run() {
		MTransformJButton.this.setMessage(message);
		MTransformJButton.this.setTT(toolTip);
		MTransformJButton.this.setEnabled(isEnabled);
	} } );
    }

    public void setDeployableView(){ updateView(null, "Ready to be Installed into Rack.", true); }
    public void setProcurableView(){ updateView(null, "Ready to be Purchased from Store.", true); }
    public void setDeployedView(){ updateView(null, "Installed into rack.", false); }

    public void setDeployingView(){ updateView("installing", "Installing.", false); }
    public void setProcuringView(){ updateView("purchasing", "Purchasing.", false); }
    public void setRemovingFromToolboxView(){ updateView("removing", "Removing from Toolbox.", false); }
    public void setRemovingFromRackView(){ updateView("removing", "Removing from Rack.", false); }

    public void setFailedInitView(){ updateView(null, "Failed Graphical Initialization.", false); }
    public void setFailedProcureView(){ updateView(null, "Failed Purchase.", true); }
    public void setFailedDeployView(){ updateView(null, "Failed Installation.", true); }
    public void setFailedRemoveFromToolboxView(){ updateView(null, "Failed Removal from Toolbox.", true); }
    public void setFailedRemoveFromRackView(){ updateView(null, "Failed Removal from Rack.", false); }
    /////////////////////////////


    // VIEW UPDATE HELPERS //////////////////
    public void setTT(String status){
	this.setToolTipText( "<html>" + "<b>Description:</b><br>" + toolTipString + "<br><br>" + "<b>Status:</b><br>" + status + "</html>");
    }

    public void setMessage(String message){
        if(message == null){
            statusJLabel.setVisible(false);
	    statusJProgressBar.setIndeterminate(false);
	    statusJProgressBar.setVisible(false);
        }
        else{
            statusJLabel.setText(message);
            statusJLabel.setVisible(true);
	    statusJProgressBar.setIndeterminate(true);
	    statusJProgressBar.setVisible(true);
        }
    }

    public void setEnabled(boolean enabled){
        super.setEnabled(enabled);
        organizationIconJLabel.setEnabled(enabled);
        descriptionIconJLabel.setEnabled(enabled);
        nameJLabel.setEnabled(enabled);
    }
    ///////////////////////////////////

    // PROCURE, DEPLOY, AND REMOVAL ////////////////
    public void install(){
        if(Util.getIsDemo())
            return;
        new InstallThread();
    }

    public void uninstall(){
        if(Util.getIsDemo())
            return;
        new UninstallThread();
    }

    public void purchase(){
        if(Util.getIsDemo())
            return;
        new PurchaseThread();
    }

    private class InstallThread extends Thread {

        public InstallThread(){
	    this.setContextClassLoader( Util.getClassLoader() );
	    MTransformJButton.this.setDeployingView();
	    this.start();
        }
	
        public void run(){
	    try{
		Util.getMPipelineJPanel().addTransform(MTransformJButton.this.getName());  // add to the rack
                Util.getMPipelineJPanel().loadAllCasings(false); // to load casings, but not their GUIs
		// LET THE USER KNOW WERE DONE
		MTransformJButton.this.setDeployedView();
	    }
	    catch(Exception e){
		try{
		    Util.handleExceptionWithRestart("Error installing transform",  e);
		}
		catch(Exception f){
                        Util.handleExceptionNoRestart("Error installing transform",  f);
			MTransformJButton.this.setFailedDeployView();
			new MOneButtonJDialog(MTransformJButton.this.getDisplayName(), "A problem occurred while installing:<br>" + MTransformJButton.this.getDisplayName() + "<br>Please contact Metavize support.");
		}
	    }
        }
    }
    

    private class PurchaseThread extends Thread {

        public PurchaseThread(){
	    this.setContextClassLoader( Util.getClassLoader() );
	    MTransformJButton.this.setProcuringView();
	    this.start();
	}    

	public void run() {	    
	    try{  // START TO DOWNLOAD THE TRANSFORM
		int dashIndex = MTransformJButton.this.getName().indexOf('-');
		Util.getToolboxManager().install(MTransformJButton.this.getName().substring(0, dashIndex));
		Util.getMMainJFrame().addMTransformJButtonToToolbox(MTransformJButton.this);
		// LET THE USER KNOW WERE DONE
		MTransformJButton.this.setDeployableView();
		Util.getMMainJFrame().focusInToolbox(MTransformJButton.this);
	    }
	    catch(Exception e){
		try{
		    Util.handleExceptionWithRestart("error purchasing transform: " +  MTransformJButton.this.getName(),  e);
		}
		catch(Exception f){
		    Util.handleExceptionNoRestart("Error purchasing transform:", f);
		    MTransformJButton.this.setFailedProcureView();
		    new MOneButtonJDialog(MTransformJButton.this.getDisplayName(), "A problem occurred while procuring:<br>" + MTransformJButton.this.getDisplayName() + "<br>Please check your network settings.");
		}
	    }	    
	}
    }
    

    private class UninstallThread extends Thread {

        public UninstallThread(){
	    this.setContextClassLoader( Util.getClassLoader() );
	    MTransformJButton.this.setRemovingFromToolboxView();
	    this.start();
	}

        public void run() {
	    try{
		int dashIndex = MTransformJButton.this.getName().indexOf('-');
		Util.getToolboxManager().uninstall(MTransformJButton.this.getName().substring(0, dashIndex));
		Util.getMMainJFrame().addMTransformJButtonToStore(MTransformJButton.this);
		// LET THE USER KNOW WE ARE DONE
		MTransformJButton.this.setProcurableView();
	    }
	    catch(Exception e){
		try{
		    Util.handleExceptionWithRestart("error removing transform: " +  MTransformJButton.this.getName(),  e);
		}
		catch(Exception f){
		    Util.handleExceptionNoRestart("Error removing transform:", f);
		    MTransformJButton.this.setFailedRemoveFromToolboxView();
		    new MOneButtonJDialog(MTransformJButton.this.getDisplayName(), "A problem occurred while removing:<br>" + MTransformJButton.this.getDisplayName() + "<br>Please contact Metavize support.");
		}
	    }

        }
    }
    ///////////////////////////////////////

    private class FadeTask implements ActionListener{
	private float fadeLeft = 1f;
	private javax.swing.Timer fadeTimer;
	private int redInit;
	private int greenInit;
	private int blueInit;
	public FadeTask(){
	    fadeTimer = new javax.swing.Timer(FADE_DELAY_MILLIS, this);
	    redInit = BACKGROUND_COLOR.getRed();
	    greenInit = BACKGROUND_COLOR.getGreen();
	    blueInit = BACKGROUND_COLOR.getBlue();
	    fadeTimer.start();
	}
	public void actionPerformed(ActionEvent evt){
	    if( fadeLeft == 1f ){
		MTransformJButton.this.setBackground(new Color(0, 0, 255));
	    }
	    else if ( fadeLeft > 0f ) {
		MTransformJButton.this.setBackground(new Color( (int)(((float)redInit)*(1f-fadeLeft)), 
								(int)(((float)greenInit)*(1f-fadeLeft)), 
								blueInit + (int)((255f-(float)blueInit)*fadeLeft) ));
	    }
	    else{
		MTransformJButton.this.setBackground(BACKGROUND_COLOR);
		fadeTimer.stop();
	    }
	    fadeLeft -= FADE_DECREMENT;
	    if(fadeLeft<0f)
		fadeLeft = 0f;
	}
    }

}
