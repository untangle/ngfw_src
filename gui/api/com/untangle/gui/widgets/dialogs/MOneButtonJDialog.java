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

import java.awt.Container;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;

import com.untangle.gui.util.Util;

public class MOneButtonJDialog extends javax.swing.JDialog implements java.awt.event.WindowListener {

    public MOneButtonJDialog(Dialog parentDialog) {
        super(parentDialog, true);
        init(parentDialog);
    }

    public MOneButtonJDialog(Frame parentFrame) {
        super(parentFrame, true);
        init(parentFrame);
    }

    public static MOneButtonJDialog factory(Container topLevelContainer, String applianceName, String warning, String title, String subtitle){
        return factory( (Window)topLevelContainer, applianceName, warning, title, subtitle);
    }

    public static MOneButtonJDialog factory(Window topLevelWindow, String applianceName, String warning, String title, String subtitle){
        if( topLevelWindow instanceof Dialog )
            return new MOneButtonJDialog((Dialog)topLevelWindow, applianceName, warning, title, subtitle);
        else if( topLevelWindow instanceof Frame )
            return new MOneButtonJDialog((Frame)topLevelWindow, applianceName, warning, title, subtitle);
        else
            return null;
    }

    public MOneButtonJDialog(Dialog topLevelDialog, String applianceName, String warning, String title, String subtitle){
        super(topLevelDialog, true);
        init(topLevelDialog);
        setTitle(title);
        labelJLabel.setText(subtitle);
        messageJLabel.setText("<html><center>" + warning + "</center></html>");
        setVisible(true);
    }

    public MOneButtonJDialog(Frame topLevelFrame, String applianceName, String warning, String title, String subtitle){
        super(topLevelFrame, true);
        init(topLevelFrame);
        setTitle(title);
        labelJLabel.setText(subtitle);
        messageJLabel.setText("<html><center>" + warning + "</center></html>");
        setVisible(true);
    }


    private void init(Window window){
        initComponents();
        addWindowListener(this);
        setBounds( Util.generateCenteredBounds(window, this.getWidth(), this.getHeight()) );
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
                proceedJButton.setFont(new java.awt.Font("Default", 0, 12));
                proceedJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/Button_Close_Dialog_106x17.png")));
                proceedJButton.setDoubleBuffered(true);
                proceedJButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
                proceedJButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
                proceedJButton.setMaximumSize(new java.awt.Dimension(125, 25));
                proceedJButton.setMinimumSize(new java.awt.Dimension(125, 25));
                proceedJButton.setPreferredSize(new java.awt.Dimension(125, 25));
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
                messageJLabel.setText("<html><center>\nYou must now shut down the Untangle Client.<br>\n<br>\nYou can log in again after shutting down, after a brief period.<br>\n</center></html>");
                messageJLabel.setDoubleBuffered(true);
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
                gridBagConstraints.insets = new java.awt.Insets(60, 30, 0, 30);
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
                setBounds((screenSize.width-456)/2, (screenSize.height-247)/2, 456, 247);
        }//GEN-END:initComponents

    private void proceedJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_proceedJButtonActionPerformed
        windowClosing(null);
    }//GEN-LAST:event_proceedJButtonActionPerformed


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
        private javax.swing.JLabel labelJLabel;
        protected javax.swing.JLabel messageJLabel;
        private javax.swing.JButton proceedJButton;
        // End of variables declaration//GEN-END:variables

}
