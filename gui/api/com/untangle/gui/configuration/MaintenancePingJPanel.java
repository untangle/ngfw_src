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

import java.awt.*;
import javax.swing.*;

import com.untangle.gui.transform.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.mvvm.*;
import com.untangle.mvvm.networking.ping.*;
import com.untangle.mvvm.security.*;
import com.untangle.mvvm.snmp.*;
import com.untangle.mvvm.tran.*;

public class MaintenancePingJPanel extends javax.swing.JPanel {


    public MaintenancePingJPanel() {
        initComponents();
        jScrollPane1.getVerticalScrollBar().setFocusable(false);
        Util.addPanelFocus(this, pingJTextField);
        Util.addFocusHighlight(pingJTextField);
        Util.addFocusHighlight(pingJEditorPane);
    }


    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        snmpButtonGroup = new javax.swing.ButtonGroup();
        trapButtonGroup = new javax.swing.ButtonGroup();
        externalRemoteJPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        restrictIPJPanel = new javax.swing.JPanel();
        internalAddressJLabel = new javax.swing.JLabel();
        pingJTextField = new javax.swing.JTextField();
        pingJButton = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        pingJEditorPane = new javax.swing.JEditorPane();
        clearJButton = new javax.swing.JButton();

        setLayout(new java.awt.GridBagLayout());

        setMaximumSize(new java.awt.Dimension(563, 343));
        setMinimumSize(new java.awt.Dimension(563, 343));
        setPreferredSize(new java.awt.Dimension(563, 343));
        externalRemoteJPanel.setLayout(new java.awt.GridBagLayout());

        externalRemoteJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Ping Test", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
        externalRemoteJPanel.setMinimumSize(new java.awt.Dimension(88, 323));
        externalRemoteJPanel.setPreferredSize(new java.awt.Dimension(730, 323));
        jLabel1.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel1.setText("<html>The <b>Ping Test</b> can be used to test that a particular host or client can be contacted from Untangle.</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 10, 10);
        externalRemoteJPanel.add(jLabel1, gridBagConstraints);

        restrictIPJPanel.setLayout(new java.awt.GridBagLayout());

        internalAddressJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        internalAddressJLabel.setText("IP Address or Hostname: ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        restrictIPJPanel.add(internalAddressJLabel, gridBagConstraints);

        pingJTextField.setMaximumSize(new java.awt.Dimension(150, 19));
        pingJTextField.setMinimumSize(new java.awt.Dimension(150, 19));
        pingJTextField.setPreferredSize(new java.awt.Dimension(150, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        restrictIPJPanel.add(pingJTextField, gridBagConstraints);

        pingJButton.setFont(new java.awt.Font("Dialog", 0, 12));
        pingJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/IconTest_16x16.png")));
        pingJButton.setText("Ping Test");
        pingJButton.setMargin(new java.awt.Insets(4, 8, 4, 8));
        pingJButton.setMaximumSize(null);
        pingJButton.setMinimumSize(null);
        pingJButton.setPreferredSize(null);
        pingJButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    pingJButtonActionPerformed(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        restrictIPJPanel.add(pingJButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.ipadx = 25;
        gridBagConstraints.insets = new java.awt.Insets(15, 0, 10, 0);
        externalRemoteJPanel.add(restrictIPJPanel, gridBagConstraints);

        jScrollPane1.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        jScrollPane1.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        pingJEditorPane.setEditable(false);
        jScrollPane1.setViewportView(pingJEditorPane);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 10, 10);
        externalRemoteJPanel.add(jScrollPane1, gridBagConstraints);

        clearJButton.setFont(new java.awt.Font("Dialog", 0, 12));
        clearJButton.setText("<html><b>Clear</b> output above</html>");
        clearJButton.setMaximumSize(new java.awt.Dimension(146, 25));
        clearJButton.setMinimumSize(new java.awt.Dimension(146, 25));
        clearJButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    clearJButtonActionPerformed(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
        externalRemoteJPanel.add(clearJButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        add(externalRemoteJPanel, gridBagConstraints);

    }//GEN-END:initComponents

    private void clearJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearJButtonActionPerformed
        SwingUtilities.invokeLater( new Runnable(){ public void run(){
            pingJEditorPane.setText("");
        }});
    }//GEN-LAST:event_clearJButtonActionPerformed

    private void pingJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pingJButtonActionPerformed
        new PingThread();
    }//GEN-LAST:event_pingJButtonActionPerformed

    public class PingThread extends Thread {
        private MProgressJDialog progress;
        private String target;
        public PingThread(){
            setName("MV-Client: PingThread");
            setDaemon(true);
            pingJTextField.setBackground( Color.WHITE );
            target = pingJTextField.getText();
            start();
            progress = MProgressJDialog.factory("Ping Test", "Please wait a few seconds...", (Window) MaintenancePingJPanel.this.getTopLevelAncestor());
            progress.getJProgressBar().setString("Testing...");
            progress.getJProgressBar().setIndeterminate(true);
            progress.getJProgressBar().setValue(1);
            progress.setVisible(true);
        }

        public void run(){
            PingResult pingResult = null;
            try{
                pingResult = Util.getPingManager().ping(target);
            }
            catch(ValidateException v){
                SwingUtilities.invokeLater( new Runnable(){ public void run(){
                    pingJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
                    progress.setVisible(false);
                    MOneButtonJDialog.factory((Window)MaintenancePingJPanel.this.getTopLevelAncestor(),
                                              "Ping Test", "You must enter a valid hostname or IP address.",
                                              "Ping Test Warning", "Warning");
                }});
                return;
            }
            catch(Exception n){
                SwingUtilities.invokeLater( new Runnable(){ public void run(){
                    pingJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
                    progress.setVisible(false);
                    MOneButtonJDialog.factory((Window)MaintenancePingJPanel.this.getTopLevelAncestor(),
                                              "Ping Test", "This test failed for an unknown reason.  Please try again.",
                                              "Ping Test Warning", "Warning");
                }});
                n.printStackTrace();
                return;
            }
            final PingResult finalPingResult = pingResult;
            SwingUtilities.invokeLater( new Runnable(){ public void run(){
                progress.setVisible(false);
                pingJEditorPane.setText( pingJEditorPane.getText() + finalPingResult.toString() + "\n");
            }});
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton clearJButton;
    private javax.swing.JPanel externalRemoteJPanel;
    private javax.swing.JLabel internalAddressJLabel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton pingJButton;
    private javax.swing.JEditorPane pingJEditorPane;
    public javax.swing.JTextField pingJTextField;
    private javax.swing.JPanel restrictIPJPanel;
    private javax.swing.ButtonGroup snmpButtonGroup;
    private javax.swing.ButtonGroup trapButtonGroup;
    // End of variables declaration//GEN-END:variables


}
