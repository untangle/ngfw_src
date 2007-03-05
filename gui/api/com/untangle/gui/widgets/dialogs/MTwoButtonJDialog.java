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

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;

import com.untangle.gui.util.Util;

public class MTwoButtonJDialog extends javax.swing.JDialog implements java.awt.event.WindowListener {

    private boolean isProceeding = false;

    public static MTwoButtonJDialog factory(Window topLevelWindow, String applianceName, String warning, String title, String subtitle){
        if( topLevelWindow instanceof Dialog )
            return new MTwoButtonJDialog((Dialog)topLevelWindow, applianceName, warning, title, subtitle);
        else if( topLevelWindow instanceof Frame )
            return new MTwoButtonJDialog((Frame)topLevelWindow, applianceName, warning, title, subtitle);
        else
            return null;
    }

    public MTwoButtonJDialog(Dialog topLevelDialog, String applianceName, String warning, String title, String subtitle){
        super(topLevelDialog, true);
        init(topLevelDialog);
        setTitle(title);
        labelJLabel.setText(subtitle);
        messageJLabel.setText("<html><center>" + warning + "</center></html>");
        setCancelText("<html><b>Cancel</b></html>");
        setProceedText("<html><b>Continue</b></html>");
    }

    public MTwoButtonJDialog(Frame topLevelFrame, String applianceName, String warning, String title, String subtitle){
        super(topLevelFrame, true);
        init(topLevelFrame);
        setTitle(title);
        labelJLabel.setText(subtitle);
        messageJLabel.setText("<html><center>" + warning + "</center></html>");
        setCancelText("<html><b>Cancel</b></html>");
        setProceedText("<html><b>Continue</b></html>");
    }

    public MTwoButtonJDialog(Dialog topLevelDialog) {
        super( topLevelDialog, true);
        init( topLevelDialog );
    }

    public MTwoButtonJDialog(Frame topLevelFrame) {
        super( topLevelFrame , true);
        init( topLevelFrame );
    }

    private void init(Window window){
        initComponents();
        addWindowListener(this);
        setBounds( Util.generateCenteredBounds(window, this.getWidth(), this.getHeight()) );
    }

    public void setProceedText(String text){
        proceedJButton.setText(text);
        proceedJButton.setIcon(null);
    }

    public void setCancelText(String text){
        cancelJButton.setText(text);
        cancelJButton.setIcon(null);
    }

        private void initComponents() {//GEN-BEGIN:initComponents
                java.awt.GridBagConstraints gridBagConstraints;

                cancelJButton = new javax.swing.JButton();
                proceedJButton = new javax.swing.JButton();
                messageJLabel = new javax.swing.JLabel();
                labelJLabel = new javax.swing.JLabel();
                backgroundJLabel = new com.untangle.gui.widgets.MTiledIconLabel();

                getContentPane().setLayout(new java.awt.GridBagLayout());

                setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
                setTitle("Confirm Upgrade...");
                setModal(true);
                setResizable(false);
                cancelJButton.setFont(new java.awt.Font("Default", 0, 12));
                cancelJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/Button_Cancel_Upgrade_106x17.png")));
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

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 13, 180);
                getContentPane().add(cancelJButton, gridBagConstraints);

                proceedJButton.setFont(new java.awt.Font("Default", 0, 12));
                proceedJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/Button_Continue_Upgrade_106x17.png")));
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

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
                gridBagConstraints.insets = new java.awt.Insets(0, 180, 13, 0);
                getContentPane().add(proceedJButton, gridBagConstraints);

                messageJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                messageJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                messageJLabel.setText("<html><center>\nThis upgrade requires that the Untangle Client be<br>\nautomatically shut down.  The Untangle Server may also be<br>\nautomatically restarted.  You may log in again after a restart.<br>\n<br>\nWould you like to continue with this upgrade?\n</center></html>");
                messageJLabel.setDoubleBuffered(true);
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
                gridBagConstraints.insets = new java.awt.Insets(40, 30, 0, 30);
                getContentPane().add(messageJLabel, gridBagConstraints);

                labelJLabel.setFont(new java.awt.Font("Dialog", 1, 24));
                labelJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                labelJLabel.setText("Warning:");
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
                setBounds((screenSize.width-466)/2, (screenSize.height-200)/2, 466, 200);
        }//GEN-END:initComponents

    private void proceedJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_proceedJButtonActionPerformed
        isProceeding = true;
        windowClosing(null);
    }//GEN-LAST:event_proceedJButtonActionPerformed

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

    public boolean isProceeding(){
        return isProceeding;
    }

        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JLabel backgroundJLabel;
        protected javax.swing.JButton cancelJButton;
        private javax.swing.JLabel labelJLabel;
        protected javax.swing.JLabel messageJLabel;
        protected javax.swing.JButton proceedJButton;
        // End of variables declaration//GEN-END:variables

}
