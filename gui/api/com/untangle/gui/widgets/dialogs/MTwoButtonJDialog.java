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
	private Window window;
	
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
    }

    public MTwoButtonJDialog(Frame topLevelFrame, String applianceName, String warning, String title, String subtitle){
        super(topLevelFrame, true);
        init(topLevelFrame);
        setTitle(title);
        labelJLabel.setText(subtitle);
        messageJLabel.setText("<html>" + warning + "</html>");
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
		this.window = window;
    }

    public void setProceedText(String text){
        proceedJButton.setText(text);
    }

    public void setCancelText(String text){
        cancelJButton.setText(text);
    }

	public void setVisible(boolean isVisible){
		if(isVisible){
			pack();
			setBounds( Util.generateCenteredBounds(window, this.getWidth(),this.getHeight() )); 			
		}
		super.setVisible(isVisible);
		if(!isVisible){
			dispose();
		}
	}
        private void initComponents() {//GEN-BEGIN:initComponents
                java.awt.GridBagConstraints gridBagConstraints;

                iconJLabel = new javax.swing.JLabel();
                dividerJPanel = new javax.swing.JPanel();
                jPanel1 = new javax.swing.JPanel();
                labelJLabel = new javax.swing.JLabel();
                messageJLabel = new javax.swing.JLabel();
                jPanel2 = new javax.swing.JPanel();
                cancelJButton = new javax.swing.JButton();
                proceedJButton = new javax.swing.JButton();
                backgroundJLabel = new com.untangle.gui.widgets.MTiledIconLabel();

                getContentPane().setLayout(new java.awt.GridBagLayout());

                setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
                setTitle("Confirm Upgrade...");
                setModal(true);
                setResizable(false);
                iconJLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/IconDialogAttention_96x96.png")));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
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
                gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(20, 0, 20, 0);
                getContentPane().add(dividerJPanel, gridBagConstraints);

                jPanel1.setLayout(new java.awt.GridBagLayout());

                jPanel1.setOpaque(false);
                labelJLabel.setFont(new java.awt.Font("Dialog", 1, 24));
                labelJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                labelJLabel.setText("Warning:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 20, 0);
                jPanel1.add(labelJLabel, gridBagConstraints);

                messageJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                messageJLabel.setText("<html>\n<center>\nThis upgrade requires that the Untangle Client be<br>\nautomatically shut down.  The Untangle Server may also be<br>\nautomatically restarted.  You may log in again after a restart.<br>\n<br>\nWould you like to continue with this upgrade?\n</center>\n</html>");
                messageJLabel.setMaximumSize(null);
                messageJLabel.setMinimumSize(null);
                messageJLabel.setPreferredSize(null);
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 20, 0);
                jPanel1.add(messageJLabel, gridBagConstraints);

                jPanel2.setLayout(new java.awt.GridBagLayout());

                jPanel2.setOpaque(false);
                cancelJButton.setFont(new java.awt.Font("Default", 0, 12));
                cancelJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/IconCancel_16x16.png")));
                cancelJButton.setText("Cancel");
                cancelJButton.setMargin(new java.awt.Insets(4, 8, 4, 8));
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
                proceedJButton.setText("Continue");
                proceedJButton.setMargin(new java.awt.Insets(4, 8, 4, 8));
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
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 2;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                jPanel1.add(jPanel2, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 2;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(20, 20, 20, 20);
                getContentPane().add(jPanel1, gridBagConstraints);

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
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                getContentPane().add(backgroundJLabel, gridBagConstraints);

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
        private javax.swing.JPanel dividerJPanel;
        private javax.swing.JLabel iconJLabel;
        private javax.swing.JPanel jPanel1;
        private javax.swing.JPanel jPanel2;
        private javax.swing.JLabel labelJLabel;
        protected javax.swing.JLabel messageJLabel;
        protected javax.swing.JButton proceedJButton;
        // End of variables declaration//GEN-END:variables

}
