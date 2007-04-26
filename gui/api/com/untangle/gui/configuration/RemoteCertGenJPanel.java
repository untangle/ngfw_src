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

import com.untangle.gui.util.Util;
import com.untangle.gui.widgets.dialogs.MConfigJDialog;

import java.awt.Window;

public class RemoteCertGenJPanel extends javax.swing.JPanel {
    
    private MConfigJDialog mConfigJDialog;

    public RemoteCertGenJPanel(MConfigJDialog mConfigJDialog){
        this.mConfigJDialog = mConfigJDialog;
        initComponents();
        Util.addPanelFocus(this, selfSignedJButton);
    }

        private void initComponents() {//GEN-BEGIN:initComponents
                java.awt.GridBagConstraints gridBagConstraints;

                selfSignedJPanel = new javax.swing.JPanel();
                selfSignedJButton = new javax.swing.JButton();
                jLabel2 = new javax.swing.JLabel();
                trustedJPanel = new javax.swing.JPanel();
                generateCSRJButton = new javax.swing.JButton();
                generateCSRJLabel = new javax.swing.JLabel();
                importCSRJButton = new javax.swing.JButton();
                importCSRJLabel = new javax.swing.JLabel();
                jSeparator1 = new javax.swing.JSeparator();

                setLayout(new java.awt.GridBagLayout());

                setMaximumSize(new java.awt.Dimension(563, 270));
                setMinimumSize(new java.awt.Dimension(563, 270));
                setPreferredSize(new java.awt.Dimension(563, 270));
                selfSignedJPanel.setLayout(new java.awt.GridBagLayout());

                selfSignedJPanel.setBorder(new javax.swing.border.EtchedBorder());
                selfSignedJButton.setFont(new java.awt.Font("Dialog", 0, 12));
                selfSignedJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/IconAction_32x32.png")));
                selfSignedJButton.setText("<html>Generate a<br><b>Self-Signed Certificate</b></html>");
                selfSignedJButton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
                selfSignedJButton.setMargin(new java.awt.Insets(4, 8, 4, 8));
                selfSignedJButton.setMaximumSize(new java.awt.Dimension(200, 60));
                selfSignedJButton.setMinimumSize(new java.awt.Dimension(200, 60));
                selfSignedJButton.setPreferredSize(new java.awt.Dimension(200, 60));
                selfSignedJButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                selfSignedJButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 0);
                selfSignedJPanel.add(selfSignedJButton, gridBagConstraints);

                jLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel2.setText("<html>\nClick this button if you have been using a signed certificate, and you want to go back to using a self-signed certificate.\n</html>");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(5, 10, 5, 5);
                selfSignedJPanel.add(jLabel2, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 0.5;
                gridBagConstraints.insets = new java.awt.Insets(0, 40, 20, 40);
                add(selfSignedJPanel, gridBagConstraints);

                trustedJPanel.setLayout(new java.awt.GridBagLayout());

                trustedJPanel.setBorder(new javax.swing.border.EtchedBorder());
                generateCSRJButton.setFont(new java.awt.Font("Dialog", 0, 12));
                generateCSRJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/IconAction_32x32.png")));
                generateCSRJButton.setText("<html>Generate a<br><b>Certificate Signature<br>Request</b></html>");
                generateCSRJButton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
                generateCSRJButton.setMargin(new java.awt.Insets(4, 8, 4, 8));
                generateCSRJButton.setMaximumSize(new java.awt.Dimension(200, 60));
                generateCSRJButton.setMinimumSize(new java.awt.Dimension(200, 60));
                generateCSRJButton.setPreferredSize(new java.awt.Dimension(200, 60));
                generateCSRJButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                generateCSRJButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 0);
                trustedJPanel.add(generateCSRJButton, gridBagConstraints);

                generateCSRJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                generateCSRJLabel.setText("<html>Click this button to generate a certificate signature request, which you can then copy and paste for use by certificate authorities such as Thawte, Verisign, etc.</html>");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(5, 10, 5, 5);
                trustedJPanel.add(generateCSRJLabel, gridBagConstraints);

                importCSRJButton.setFont(new java.awt.Font("Dialog", 0, 12));
                importCSRJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/IconAction_32x32.png")));
                importCSRJButton.setText("<html>Import a<br><b>Signed Certificate</b></html>");
                importCSRJButton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
                importCSRJButton.setMargin(new java.awt.Insets(4, 8, 4, 8));
                importCSRJButton.setMaximumSize(new java.awt.Dimension(200, 60));
                importCSRJButton.setMinimumSize(new java.awt.Dimension(200, 60));
                importCSRJButton.setPreferredSize(new java.awt.Dimension(200, 60));
                importCSRJButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                importCSRJButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 2;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 0);
                trustedJPanel.add(importCSRJButton, gridBagConstraints);

                importCSRJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                importCSRJLabel.setText("<html>\nClick this button to import a signed certificate which has been generated by a certificate authority, and was based on a previous signature request from Untangle.\n</html>");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 2;
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(5, 10, 5, 5);
                trustedJPanel.add(importCSRJLabel, gridBagConstraints);

                jSeparator1.setForeground(new java.awt.Color(166, 166, 166));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.gridwidth = 2;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                trustedJPanel.add(jSeparator1, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 0.5;
                gridBagConstraints.insets = new java.awt.Insets(20, 40, 0, 40);
                add(trustedJPanel, gridBagConstraints);

        }//GEN-END:initComponents

    private void importCSRJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importCSRJButtonActionPerformed
	if( Util.getIsDemo() )
	    return;
	importCSRJButton.setEnabled(false);
	RemoteCertImportTrustedJDialog.factory((Window)this.getTopLevelAncestor(), mConfigJDialog).setVisible(true);
	importCSRJButton.setEnabled(true);
    }//GEN-LAST:event_importCSRJButtonActionPerformed
    
    private void selfSignedJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selfSignedJButtonActionPerformed
	if( Util.getIsDemo() )
	    return;
	selfSignedJButton.setEnabled(false);
	RemoteCertGenSelfSignedJDialog.factory((Window)this.getTopLevelAncestor(),mConfigJDialog).setVisible(true);
	selfSignedJButton.setEnabled(true);
    }//GEN-LAST:event_selfSignedJButtonActionPerformed
    
    private void generateCSRJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_generateCSRJButtonActionPerformed
	if( Util.getIsDemo() )
	    return;
	generateCSRJButton.setEnabled(false);
	RemoteCertGenTrustedJDialog.factory((Window)this.getTopLevelAncestor()).setVisible(true);
	generateCSRJButton.setEnabled(true);
    }//GEN-LAST:event_generateCSRJButtonActionPerformed
    
    
        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JButton generateCSRJButton;
        private javax.swing.JLabel generateCSRJLabel;
        private javax.swing.JButton importCSRJButton;
        private javax.swing.JLabel importCSRJLabel;
        private javax.swing.JLabel jLabel2;
        private javax.swing.JSeparator jSeparator1;
        private javax.swing.JButton selfSignedJButton;
        private javax.swing.JPanel selfSignedJPanel;
        private javax.swing.JPanel trustedJPanel;
        // End of variables declaration//GEN-END:variables
    
}
