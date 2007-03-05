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

package com.untangle.gui.configuration;


import com.untangle.mvvm.security.RFC2253Name;
import com.untangle.gui.widgets.dialogs.MConfigJDialog;
import com.untangle.gui.util.Util;
import com.untangle.gui.widgets.dialogs.*;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import javax.swing.*;

import com.untangle.gui.util.Util;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.mvvm.security.RFC2253Name;

public class RemoteCertGenSelfSignedJDialog extends javax.swing.JDialog implements java.awt.event.WindowListener {
    
    private MConfigJDialog mConfigJDialog;
    

    public static RemoteCertGenSelfSignedJDialog factory(Container topLevelContainer, MConfigJDialog mConfigJDialog){       
	RemoteCertGenSelfSignedJDialog remoteCertGenSelfSignedJDialog;
	if(topLevelContainer instanceof Frame)
	    remoteCertGenSelfSignedJDialog = new RemoteCertGenSelfSignedJDialog((Frame)topLevelContainer, mConfigJDialog);
	else
	    remoteCertGenSelfSignedJDialog = new RemoteCertGenSelfSignedJDialog((Dialog)topLevelContainer, mConfigJDialog);
	return remoteCertGenSelfSignedJDialog;
    }

    public RemoteCertGenSelfSignedJDialog(Dialog topLevelDialog, MConfigJDialog mConfigJDialog) {
        super(topLevelDialog, true);
	init(topLevelDialog, mConfigJDialog);	
    }

    public RemoteCertGenSelfSignedJDialog(Frame topLevelFrame, MConfigJDialog mConfigJDialog) {
        super(topLevelFrame, true);
	init(topLevelFrame, mConfigJDialog);
    }

    private void init(Window topLevelWindow, MConfigJDialog mConfigJDialog) {
	this.mConfigJDialog = mConfigJDialog;
        initComponents();
        this.addWindowListener(this);
        this.setBounds( Util.generateCenteredBounds(topLevelWindow.getBounds(), this.getWidth(), this.getHeight()) );
	new RefreshThread();
    }

        private void initComponents() {//GEN-BEGIN:initComponents
                buttonGroup1 = new javax.swing.ButtonGroup();
                cancelJButton = new javax.swing.JButton();
                proceedJButton = new javax.swing.JButton();
                messageJLabel = new javax.swing.JLabel();
                labelJLabel = new javax.swing.JLabel();
                organizationJLabel = new javax.swing.JLabel();
                organizationJTextField = new javax.swing.JTextField();
                organizationUnitJLabel = new javax.swing.JLabel();
                organizationUnitJTextField = new javax.swing.JTextField();
                cityJLabel = new javax.swing.JLabel();
                cityJTextField = new javax.swing.JTextField();
                stateJLabel = new javax.swing.JLabel();
                stateJTextField = new javax.swing.JTextField();
                countryJLabel = new javax.swing.JLabel();
                countryJTextField = new javax.swing.JTextField();
                hostnameJLabel = new javax.swing.JLabel();
                hostnameJTextField = new javax.swing.JTextField();
                jProgressBar = new javax.swing.JProgressBar();
                backgroundJLabel = new com.untangle.gui.widgets.MTiledIconLabel();

                getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

                setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
                setTitle("Certificate Generation");
                setModal(true);
                setResizable(false);
                cancelJButton.setFont(new java.awt.Font("Default", 0, 12));
                cancelJButton.setText("<html><b>Cancel</b></html>");
                cancelJButton.setDoubleBuffered(true);
                cancelJButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
                cancelJButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
                cancelJButton.setMaximumSize(new java.awt.Dimension(130, 25));
                cancelJButton.setMinimumSize(new java.awt.Dimension(130, 25));
                cancelJButton.setPreferredSize(new java.awt.Dimension(130, 25));
                cancelJButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                cancelJButtonActionPerformed(evt);
                        }
                });

                getContentPane().add(cancelJButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 340, -1, -1));

                proceedJButton.setFont(new java.awt.Font("Default", 0, 12));
                proceedJButton.setText("<html><b>Proceed</b></html>");
                proceedJButton.setDoubleBuffered(true);
                proceedJButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
                proceedJButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
                proceedJButton.setMaximumSize(new java.awt.Dimension(150, 25));
                proceedJButton.setMinimumSize(new java.awt.Dimension(150, 25));
                proceedJButton.setPreferredSize(new java.awt.Dimension(150, 25));
                proceedJButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                proceedJButtonActionPerformed(evt);
                        }
                });

                getContentPane().add(proceedJButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(240, 340, -1, -1));

                messageJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                messageJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                messageJLabel.setText("<html>Please fill out the following fields, which will be used to generate your self-signed certificate.</html>");
                messageJLabel.setDoubleBuffered(true);
                getContentPane().add(messageJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(90, 40, 300, -1));

                labelJLabel.setFont(new java.awt.Font("Dialog", 1, 24));
                labelJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                labelJLabel.setText("Generate Self-Signed Certificate");
                labelJLabel.setDoubleBuffered(true);
                getContentPane().add(labelJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 10, 456, -1));

                organizationJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                organizationJLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
                organizationJLabel.setText("Organization (O):");
                getContentPane().add(organizationJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 100, 150, -1));

                getContentPane().add(organizationJTextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(190, 100, 200, -1));

                organizationUnitJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                organizationUnitJLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
                organizationUnitJLabel.setText("Organization Unit (OU):");
                getContentPane().add(organizationUnitJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 130, 150, -1));

                getContentPane().add(organizationUnitJTextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(190, 130, 200, -1));

                cityJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                cityJLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
                cityJLabel.setText("City (L):");
                getContentPane().add(cityJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 160, 150, -1));

                getContentPane().add(cityJTextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(190, 160, 200, -1));

                stateJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                stateJLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
                stateJLabel.setText("State (ST):");
                getContentPane().add(stateJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 190, 150, -1));

                getContentPane().add(stateJTextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(190, 190, 200, -1));

                countryJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                countryJLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
                countryJLabel.setText("Country (C):");
                getContentPane().add(countryJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 220, 150, -1));

                getContentPane().add(countryJTextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(190, 220, 200, -1));

                hostnameJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                hostnameJLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
                hostnameJLabel.setText("Hostname (CN):");
                getContentPane().add(hostnameJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 250, 150, -1));

                hostnameJTextField.setEditable(false);
                hostnameJTextField.setText("querying...");
                getContentPane().add(hostnameJTextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(190, 250, 200, -1));

                jProgressBar.setFont(new java.awt.Font("Dialog", 0, 12));
                jProgressBar.setMaximumSize(new java.awt.Dimension(32767, 20));
                jProgressBar.setMinimumSize(new java.awt.Dimension(10, 20));
                jProgressBar.setPreferredSize(new java.awt.Dimension(148, 20));
                jProgressBar.setString("");
                jProgressBar.setStringPainted(true);
                getContentPane().add(jProgressBar, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 300, 320, -1));

                backgroundJLabel.setFont(new java.awt.Font("Default", 0, 12));
                backgroundJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                backgroundJLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/LightGreyBackground1600x100.png")));
                backgroundJLabel.setDoubleBuffered(true);
                backgroundJLabel.setFocusable(false);
                backgroundJLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
                backgroundJLabel.setOpaque(true);
                getContentPane().add(backgroundJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 456, 383));

                java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
                setBounds((screenSize.width-456)/2, (screenSize.height-404)/2, 456, 404);
        }//GEN-END:initComponents

        String organization;
        String organizationUnit;
        String city;
        String state;
        String country;

    private void proceedJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_proceedJButtonActionPerformed
        // ORGANIZATION
        organization = organizationJTextField.getText().trim();
        if( organization.length() == 0) {
                organizationJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
                MOneButtonJDialog.factory(this, "Certificate Generation", "You must specify an organization.", "Certificate Generation Warning", "Warning");
                return;
        }
        else{
                organizationJTextField.setBackground( Color.WHITE );
        }

        // ORGANIZATION UNIT
        organizationUnit = organizationUnitJTextField.getText().trim();
        if( organizationUnit.length() == 0) {
                organizationUnitJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
                MOneButtonJDialog.factory(this, "Certificate Generation", "You must specify an organization unit.", "Certificate Generation Warning", "Warning");
                return;
        }
        else{
                organizationUnitJTextField.setBackground( Color.WHITE );
        }

        // CITY
        city = cityJTextField.getText().trim();
        if( city.length() == 0) {
                cityJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
                MOneButtonJDialog.factory(this, "Certificate Generation", "You must specify a city.", "Certificate Generation Warning", "Warning");
                return;
        }
        else{
                cityJTextField.setBackground( Color.WHITE );
        }

        // STATE
        state = stateJTextField.getText().trim();
        if( state.length() == 0) {
                stateJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
                MOneButtonJDialog.factory(this, "Certificate Generation", "You must specify a state.", "Certificate Generation Warning", "Warning");
                return;
        }
        else{
                stateJTextField.setBackground( Color.WHITE );
        }

        // COUNTRY
        country = countryJTextField.getText().trim();
        if( country.length() == 0) {
                countryJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
                MOneButtonJDialog.factory(this, "Certificate Generation", "You must specify a country.", "Certificate Generation Warning", "Warning");
                return;
        }
        else{
                countryJTextField.setBackground( Color.WHITE );
        }


        new CertGenerateThread();
    }//GEN-LAST:event_proceedJButtonActionPerformed

    private class CertGenerateThread extends Thread {
    public CertGenerateThread(){
	super("MVCLIENT-CertGenerateThread");
        setDaemon(true);
        jProgressBar.setIndeterminate(true);
        jProgressBar.setString("Generating Certificate");
        jProgressBar.setValue(0);
        proceedJButton.setEnabled(false);
        cancelJButton.setEnabled(false);
        start();
    }
    public void run(){
        try{
        RFC2253Name distinguishedName = new RFC2253Name(organization, organizationUnit, city, state, country);
        boolean result = Util.getAppServerManager().regenCert(distinguishedName, 5*365);
        if(!result)
            throw new Exception();
        Thread.sleep(1000);

	SwingUtilities.invokeLater( new Runnable(){ public void run(){
	    jProgressBar.setIndeterminate(false);
	    jProgressBar.setValue(100);
	    jProgressBar.setString("Certificate Successfully Generated");
	}});
	Thread.sleep(1500);
	SwingUtilities.invokeLater( new Runnable(){ public void run(){
	    RemoteCertGenSelfSignedJDialog.this.setVisible(false);
	}});
	mConfigJDialog.refreshGui();
	}
	catch(Exception e){
	    SwingUtilities.invokeLater( new Runnable(){ public void run(){
		jProgressBar.setIndeterminate(false);
		jProgressBar.setValue(100);
		jProgressBar.setString("Error. Please try again.");
		proceedJButton.setEnabled(true);
		cancelJButton.setEnabled(true);
	    }});
	    Util.handleExceptionNoRestart("Error generating self-signed certificate", e);
	}
	
    }
	
    }
    

    private class RefreshThread extends Thread {
    public RefreshThread(){
        setDaemon(true);
        start();
    }
    public void run(){
        try{
        final String hostname = Util.getNetworkManager().getHostname().toString();
        hostnameJTextField.setText(hostname);
        SwingUtilities.invokeLater( new Runnable(){ public void run(){
            hostnameJTextField.setText(hostname);
        }});
        }
        catch(Exception e){
        Util.handleExceptionNoRestart("Error querying hostname", e);
        }
    }

    }

    private void cancelJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelJButtonActionPerformed
        windowClosing(null);
    }//GEN-LAST:event_cancelJButtonActionPerformed


    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
        this.setVisible(false);
        dispose();
    }


    public void windowActivated(java.awt.event.WindowEvent windowEvent) {}
    public void windowClosed(java.awt.event.WindowEvent windowEvent) {}
    public void windowDeactivated(java.awt.event.WindowEvent windowEvent) {}
    public void windowDeiconified(java.awt.event.WindowEvent windowEvent) {}
    public void windowIconified(java.awt.event.WindowEvent windowEvent) {}
    public void windowOpened(java.awt.event.WindowEvent windowEvent) {}



        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JLabel backgroundJLabel;
        private javax.swing.ButtonGroup buttonGroup1;
        protected javax.swing.JButton cancelJButton;
        private javax.swing.JLabel cityJLabel;
        public javax.swing.JTextField cityJTextField;
        private javax.swing.JLabel countryJLabel;
        public javax.swing.JTextField countryJTextField;
        private javax.swing.JLabel hostnameJLabel;
        public javax.swing.JTextField hostnameJTextField;
        private javax.swing.JProgressBar jProgressBar;
        private javax.swing.JLabel labelJLabel;
        protected javax.swing.JLabel messageJLabel;
        private javax.swing.JLabel organizationJLabel;
        public javax.swing.JTextField organizationJTextField;
        private javax.swing.JLabel organizationUnitJLabel;
        public javax.swing.JTextField organizationUnitJTextField;
        protected javax.swing.JButton proceedJButton;
        private javax.swing.JLabel stateJLabel;
        public javax.swing.JTextField stateJTextField;
        // End of variables declaration//GEN-END:variables

}
