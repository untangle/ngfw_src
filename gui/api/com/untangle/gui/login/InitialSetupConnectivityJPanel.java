/*
 * $HeadURL:$
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

package com.untangle.gui.login;

import java.awt.Dialog;

import com.untangle.gui.configuration.NetworkConnectivityTestJDialog;
import com.untangle.gui.util.Util;
import com.untangle.gui.widgets.wizard.*;

public class InitialSetupConnectivityJPanel extends MWizardPageJPanel {


    public InitialSetupConnectivityJPanel() {
        initComponents();
    }

    public void initialFocus(){
        connectivityTestJButton.requestFocus();
    }

    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        buttonGroup1 = new javax.swing.ButtonGroup();
        contentJPanel = new javax.swing.JPanel();
        jLabel10 = new javax.swing.JLabel();
        connectivityTestJButton = new javax.swing.JButton();
        jLabel11 = new javax.swing.JLabel();
        backgroundJPabel = new javax.swing.JLabel();

        setLayout(new java.awt.GridBagLayout());

        setOpaque(false);
        contentJPanel.setLayout(new java.awt.GridBagLayout());

        contentJPanel.setOpaque(false);
        jLabel10.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel10.setText("<html>The <b>Connectivity Test is optional</b>, but it can tell you if the Untangle Server can contact DNS and the internet with the settings you have just saved.</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(15, 15, 0, 15);
        contentJPanel.add(jLabel10, gridBagConstraints);

        connectivityTestJButton.setFont(new java.awt.Font("Dialog", 0, 12));
        connectivityTestJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/IconTest_16x16.png")));
        connectivityTestJButton.setText("Connectivity Test");
        connectivityTestJButton.setMargin(new java.awt.Insets(4, 8, 4, 8));
        connectivityTestJButton.setMaximumSize(null);
        connectivityTestJButton.setMinimumSize(null);
        connectivityTestJButton.setPreferredSize(null);
        connectivityTestJButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    connectivityTestJButtonActionPerformed(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(30, 0, 0, 0);
        contentJPanel.add(connectivityTestJButton, gridBagConstraints);

        jLabel11.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel11.setText("<html>If the connectivity test does not pass, you should try different external address settings.  You may go back to the \"External Address\" page by pressing the \"Previous page\" button.</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(30, 15, 0, 15);
        contentJPanel.add(jLabel11, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(contentJPanel, gridBagConstraints);

        backgroundJPabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/login/ProductShot.png")));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
        gridBagConstraints.weightx = 1.0;
        add(backgroundJPabel, gridBagConstraints);

    }//GEN-END:initComponents

    private void connectivityTestJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_connectivityTestJButtonActionPerformed
        try{
            NetworkConnectivityTestJDialog connectivityJDialog = new NetworkConnectivityTestJDialog((Dialog)getTopLevelAncestor());
            connectivityJDialog.setVisible(true);
        }
        catch(Exception e){
            try{ Util.handleExceptionWithRestart("Error showing connectivity tester", e); }
            catch(Exception f){ Util.handleExceptionNoRestart("Error showing connectivity tester", f); }
        }
    }//GEN-LAST:event_connectivityTestJButtonActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel backgroundJPabel;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JButton connectivityTestJButton;
    private javax.swing.JPanel contentJPanel;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    // End of variables declaration//GEN-END:variables

}
