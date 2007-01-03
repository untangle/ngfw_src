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

package com.untangle.gui.widgets.dialogs;

import com.untangle.gui.util.Util;

import java.awt.Window;
import java.awt.Container;
import java.awt.Frame;
import java.awt.Dialog;

final public class RestartDialog extends javax.swing.JDialog implements java.awt.event.WindowListener {

    public static RestartDialog factory(Container parentContainer){
	if(parentContainer instanceof Dialog)
	    return new RestartDialog((Dialog)parentContainer);
	else if(parentContainer instanceof Frame)
	    return new RestartDialog((Frame)parentContainer);
	else
	    return null;
    }

    private RestartDialog(Dialog parentDialog) {
        super(parentDialog, true);
	init(parentDialog);
    }

    private RestartDialog(Frame parentFrame){
	super(parentFrame, true);
	init(parentFrame);
    }

    private void init(Window parentWindow){
	initComponents();
        addWindowListener(this);
        setBounds( Util.generateCenteredBounds(parentWindow, this.getWidth(), this.getHeight()) );
        setVisible(true);
    }

        private void initComponents() {//GEN-BEGIN:initComponents
                java.awt.GridBagConstraints gridBagConstraints;

                proceedJButton = new javax.swing.JButton();
                messageJLabel = new javax.swing.JLabel();
                labelJLabel = new javax.swing.JLabel();
                backgroundJLabel = new com.untangle.gui.widgets.MTiledIconLabel();

                getContentPane().setLayout(new java.awt.GridBagLayout());

                setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
                setTitle("Confirm Restart...");
                setModal(true);
                setResizable(false);
                proceedJButton.setFont(new java.awt.Font("Default", 1, 12));
                proceedJButton.setText("Exit");
                proceedJButton.setDoubleBuffered(true);
                proceedJButton.setFocusPainted(false);
                proceedJButton.setFocusable(false);
                proceedJButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                proceedJButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 13, 0);
                getContentPane().add(proceedJButton, gridBagConstraints);

                messageJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                messageJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                messageJLabel.setText("<html><center>\nYou must now exit this program.<br>\nYou can log in again after a brief period.<br>\n<font color=\"FF0000\"><b>DO NOT MANUALLY SHUTDOWN OR RESTART THE UNTANGLE SERVER WHILE IT IS UPGRADING!</b></font>\n</center></html>");
                messageJLabel.setDoubleBuffered(true);
                messageJLabel.setPreferredSize(new java.awt.Dimension(400, 45));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
                gridBagConstraints.insets = new java.awt.Insets(50, 30, 0, 30);
                getContentPane().add(messageJLabel, gridBagConstraints);

                labelJLabel.setFont(new java.awt.Font("Dialog", 1, 24));
                labelJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                labelJLabel.setText("Attention:");
                labelJLabel.setDoubleBuffered(true);
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
                gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
                getContentPane().add(labelJLabel, gridBagConstraints);

                backgroundJLabel.setFont(new java.awt.Font("Default", 0, 12));
                backgroundJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                backgroundJLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/LightGreyBackground1600x100.png")));
                backgroundJLabel.setDoubleBuffered(true);
                backgroundJLabel.setFocusable(false);
                backgroundJLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
                backgroundJLabel.setOpaque(true);
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                getContentPane().add(backgroundJLabel, gridBagConstraints);

                java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
                setBounds((screenSize.width-466)/2, (screenSize.height-220)/2, 466, 220);
        }//GEN-END:initComponents

    private void proceedJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_proceedJButtonActionPerformed
        windowClosing(null);
    }//GEN-LAST:event_proceedJButtonActionPerformed


    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
        this.setVisible(false);
        dispose();
        Util.exit(0);
    }


    public void windowActivated(java.awt.event.WindowEvent windowEvent) {}
    public void windowClosed(java.awt.event.WindowEvent windowEvent) {}
    public void windowDeactivated(java.awt.event.WindowEvent windowEvent) {}
    public void windowDeiconified(java.awt.event.WindowEvent windowEvent) {}
    public void windowIconified(java.awt.event.WindowEvent windowEvent) {}
    public void windowOpened(java.awt.event.WindowEvent windowEvent) {}


        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JLabel backgroundJLabel;
        private javax.swing.JLabel labelJLabel;
        private javax.swing.JLabel messageJLabel;
        private javax.swing.JButton proceedJButton;
        // End of variables declaration//GEN-END:variables

}
