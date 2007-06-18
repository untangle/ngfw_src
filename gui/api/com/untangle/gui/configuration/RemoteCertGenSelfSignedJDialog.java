/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.gui.configuration;


import java.awt.Color;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import javax.swing.*;

import com.untangle.gui.util.Util;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.uvm.security.RFC2253Name;


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
        MConfigJDialog.setInitialFocusComponent(organizationJTextField);
        Util.addFocusHighlight(organizationJTextField);
        Util.addFocusHighlight(organizationUnitJTextField);
        Util.addFocusHighlight(cityJTextField);
        Util.addFocusHighlight(stateJTextField);
        Util.addFocusHighlight(countryJTextField);
        this.addWindowListener(this);
        pack();
        this.setBounds( Util.generateCenteredBounds(topLevelWindow.getBounds(), this.getWidth(), this.getHeight()) );
        new RefreshThread();
    }

    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        buttonGroup1 = new javax.swing.ButtonGroup();
        iconJLabel = new javax.swing.JLabel();
        dividerJPanel = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        labelJLabel = new javax.swing.JLabel();
        messageJLabel = new javax.swing.JLabel();
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
        jPanel2 = new javax.swing.JPanel();
        cancelJButton = new javax.swing.JButton();
        proceedJButton = new javax.swing.JButton();
        backgroundJLabel = new com.untangle.gui.widgets.MTiledIconLabel();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Certificate Generation");
        setModal(true);
        setResizable(false);
        iconJLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/IconDialogQuestion_96x96.png")));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(20, 20, 20, 20);
        getContentPane().add(iconJLabel, gridBagConstraints);

        dividerJPanel.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(154, 154, 154)));
        dividerJPanel.setMaximumSize(new java.awt.Dimension(1, 1600));
        dividerJPanel.setMinimumSize(new java.awt.Dimension(1, 10));
        dividerJPanel.setOpaque(false);
        dividerJPanel.setPreferredSize(new java.awt.Dimension(1, 10));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(20, 0, 20, 0);
        getContentPane().add(dividerJPanel, gridBagConstraints);

        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel1.setOpaque(false);
        labelJLabel.setFont(new java.awt.Font("Dialog", 1, 24));
        labelJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        labelJLabel.setText("Generate Self-Signed Certificate");
        jPanel1.add(labelJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 10, 456, -1));

        messageJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        messageJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        messageJLabel.setText("<html>Please fill out the following fields, which will be used to generate your self-signed certificate.</html>");
        jPanel1.add(messageJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(90, 40, 300, -1));

        organizationJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        organizationJLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        organizationJLabel.setText("Organization (O):");
        jPanel1.add(organizationJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 100, 150, -1));

        jPanel1.add(organizationJTextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(190, 100, 200, -1));

        organizationUnitJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        organizationUnitJLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        organizationUnitJLabel.setText("Organization Unit (OU):");
        jPanel1.add(organizationUnitJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 130, 150, -1));

        jPanel1.add(organizationUnitJTextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(190, 130, 200, -1));

        cityJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        cityJLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        cityJLabel.setText("City (L):");
        jPanel1.add(cityJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 160, 150, -1));

        jPanel1.add(cityJTextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(190, 160, 200, -1));

        stateJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        stateJLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        stateJLabel.setText("State (ST):");
        jPanel1.add(stateJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 190, 150, -1));

        jPanel1.add(stateJTextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(190, 190, 200, -1));

        countryJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        countryJLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        countryJLabel.setText("Country (C):");
        jPanel1.add(countryJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 220, 150, -1));

        jPanel1.add(countryJTextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(190, 220, 200, -1));

        hostnameJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        hostnameJLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        hostnameJLabel.setText("Hostname (CN):");
        jPanel1.add(hostnameJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 250, 150, -1));

        hostnameJTextField.setEditable(false);
        hostnameJTextField.setText("querying...");
        hostnameJTextField.setFocusable(false);
        jPanel1.add(hostnameJTextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(190, 250, 200, -1));

        jProgressBar.setFont(new java.awt.Font("Dialog", 0, 12));
        jProgressBar.setMaximumSize(new java.awt.Dimension(32767, 20));
        jProgressBar.setMinimumSize(new java.awt.Dimension(10, 20));
        jProgressBar.setOpaque(false);
        jProgressBar.setPreferredSize(new java.awt.Dimension(148, 20));
        jProgressBar.setString("");
        jProgressBar.setStringPainted(true);
        jPanel1.add(jProgressBar, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 300, 320, -1));

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(20, 20, 20, 20);
        getContentPane().add(jPanel1, gridBagConstraints);

        jPanel2.setLayout(new java.awt.GridBagLayout());

        jPanel2.setOpaque(false);
        cancelJButton.setFont(new java.awt.Font("Default", 0, 12));
        cancelJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/IconCancel_16x16.png")));
        cancelJButton.setText("Cancel");
        cancelJButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        cancelJButton.setMargin(new java.awt.Insets(4, 8, 4, 8));
        cancelJButton.setMaximumSize(null);
        cancelJButton.setMinimumSize(null);
        cancelJButton.setOpaque(false);
        cancelJButton.setPreferredSize(null);
        cancelJButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    cancelJButtonActionPerformed(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
        jPanel2.add(cancelJButton, gridBagConstraints);

        proceedJButton.setFont(new java.awt.Font("Default", 0, 12));
        proceedJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/IconSave_23x16.png")));
        proceedJButton.setText("Proceed");
        proceedJButton.setMargin(new java.awt.Insets(4, 8, 4, 8));
        proceedJButton.setMaximumSize(null);
        proceedJButton.setMinimumSize(null);
        proceedJButton.setOpaque(false);
        proceedJButton.setPreferredSize(null);
        proceedJButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    proceedJButtonActionPerformed(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        jPanel2.add(proceedJButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 20, 0);
        getContentPane().add(jPanel2, gridBagConstraints);

        backgroundJLabel.setFont(new java.awt.Font("Default", 0, 12));
        backgroundJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        backgroundJLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/LightGreyBackground1600x100.png")));
        backgroundJLabel.setFocusable(false);
        backgroundJLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        backgroundJLabel.setOpaque(true);
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
    private javax.swing.JPanel dividerJPanel;
    private javax.swing.JLabel hostnameJLabel;
    public javax.swing.JTextField hostnameJTextField;
    private javax.swing.JLabel iconJLabel;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
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
