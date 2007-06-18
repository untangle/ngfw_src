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

import java.awt.Window;

import com.untangle.gui.util.Util;
import com.untangle.gui.widgets.dialogs.MConfigJDialog;

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
