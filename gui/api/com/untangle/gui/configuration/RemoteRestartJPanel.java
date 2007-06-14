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

package com.untangle.gui.configuration;

import java.awt.Window;

import com.untangle.gui.node.*;
import com.untangle.gui.util.Util;
import com.untangle.gui.widgets.dialogs.*;


public class RemoteRestartJPanel extends javax.swing.JPanel {

    public RemoteRestartJPanel() {
        initComponents();
        Util.addPanelFocus(this, rebootJButton);
    }



    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        serverRoutingJPanel = new javax.swing.JPanel();
        rebootJButton = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();

        setLayout(new java.awt.GridBagLayout());

        setMaximumSize(new java.awt.Dimension(563, 270));
        setMinimumSize(new java.awt.Dimension(563, 270));
        setPreferredSize(new java.awt.Dimension(563, 270));
        serverRoutingJPanel.setLayout(new java.awt.GridBagLayout());

        serverRoutingJPanel.setBorder(new javax.swing.border.EtchedBorder());
        serverRoutingJPanel.setMaximumSize(new java.awt.Dimension(1061, 64));
        serverRoutingJPanel.setMinimumSize(new java.awt.Dimension(1061, 64));
        rebootJButton.setFont(new java.awt.Font("Dialog", 0, 12));
        rebootJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/IconReboot_32x32.png")));
        rebootJButton.setText("Reboot");
        rebootJButton.setMargin(new java.awt.Insets(4, 8, 4, 8));
        rebootJButton.setMaximumSize(null);
        rebootJButton.setMinimumSize(null);
        rebootJButton.setPreferredSize(null);
        rebootJButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    rebootJButtonActionPerformed(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 0);
        serverRoutingJPanel.add(rebootJButton, gridBagConstraints);

        jLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel2.setText("<html><b>Warning: </b>Clicking this button will reboot the Untangle Server, temporarily interrupting network activity.</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 10, 5, 5);
        serverRoutingJPanel.add(jLabel2, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 50, 0, 50);
        add(serverRoutingJPanel, gridBagConstraints);

    }//GEN-END:initComponents

    private void rebootJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rebootJButtonActionPerformed
        if( Util.getIsDemo() )
            return;
        MTwoButtonJDialog warningJDialog = MTwoButtonJDialog.factory((Window)this.getTopLevelAncestor(), "",
                                                                     "You are about to manually reboot.  This will interrupt normal network operations<br>" +
                                                                     "until the Untangle Server is finished automatically restarting.<br>" +
                                                                     "This may take up to several minutes to complete.",
                                                                     "Manual Reboot Warning", "Warning");
        warningJDialog.setVisible(true);
        if( warningJDialog.isProceeding() ){
            try{
                Util.getUvmContext().rebootBox();
            }
            catch(Exception e){
                Util.handleExceptionNoRestart("Error: Unable to reboot Untangle Server", e);
            }
            Util.exit(0);
        }
    }//GEN-LAST:event_rebootJButtonActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel2;
    private javax.swing.JButton rebootJButton;
    private javax.swing.JPanel serverRoutingJPanel;
    // End of variables declaration//GEN-END:variables

}
