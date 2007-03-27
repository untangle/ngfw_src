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

import java.awt.*;
import javax.swing.*;

public class MProgressJDialog extends javax.swing.JDialog implements java.awt.event.WindowListener {
		
	private Window window;
		
    public static MProgressJDialog factory(String label, String message, Window parentWindow){
	if( parentWindow instanceof Frame )
	    return new MProgressJDialog(label, message, (Frame)parentWindow);
	else if( parentWindow instanceof Dialog )
	    return new MProgressJDialog(label, message, (Dialog)parentWindow);
	else
	    return null;
    }
    
    public MProgressJDialog(String label, String message, Frame topLevelFrame){
        super(topLevelFrame, true);
        init(label, message, topLevelFrame);
    }
    
    public MProgressJDialog(String label, String message, Dialog topLevelDialog){
        super(topLevelDialog, true);
        init(label, message, topLevelDialog);
    }
    
    protected void init(String label, String message, Window window) {
        initComponents();
        labelJLabel.setText(label);
        messageJLabel.setText(message);
        addWindowListener(this);
		this.window = window;
    }
    
    public JProgressBar getJProgressBar(){
        return jProgressBar;
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
                jProgressBar = new javax.swing.JProgressBar();
                backgroundJLabel = new com.untangle.gui.widgets.MTiledIconLabel();

                getContentPane().setLayout(new java.awt.GridBagLayout());

                setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
                setTitle("Progress...");
                setModal(true);
                setResizable(false);
                iconJLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/IconDialogWizard_96x96.png")));
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
                labelJLabel.setFocusable(false);
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 20, 0);
                jPanel1.add(labelJLabel, gridBagConstraints);

                messageJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                messageJLabel.setText("<html>\n<center>\nYou must now shut down the Untangle Client.<br>\nYou can log in again after shutting down, after a brief period.\n</center>\n</html>");
                messageJLabel.setFocusable(false);
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 20, 0);
                jPanel1.add(messageJLabel, gridBagConstraints);

                jProgressBar.setFont(new java.awt.Font("Dialog", 0, 12));
                jProgressBar.setFocusable(false);
                jProgressBar.setMaximumSize(new java.awt.Dimension(32767, 20));
                jProgressBar.setMinimumSize(new java.awt.Dimension(10, 20));
                jProgressBar.setPreferredSize(new java.awt.Dimension(148, 20));
                jProgressBar.setStringPainted(true);
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 2;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                jPanel1.add(jProgressBar, gridBagConstraints);

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
    

    public void windowClosing(java.awt.event.WindowEvent windowEvent) {}    
    public void windowActivated(java.awt.event.WindowEvent windowEvent) {}    
    public void windowClosed(java.awt.event.WindowEvent windowEvent) {}    
    public void windowDeactivated(java.awt.event.WindowEvent windowEvent) {}
    public void windowDeiconified(java.awt.event.WindowEvent windowEvent) {}
    public void windowIconified(java.awt.event.WindowEvent windowEvent) {}
    public void windowOpened(java.awt.event.WindowEvent windowEvent) {}
    
    
        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JLabel backgroundJLabel;
        private javax.swing.JPanel dividerJPanel;
        private javax.swing.JLabel iconJLabel;
        private javax.swing.JPanel jPanel1;
        protected javax.swing.JProgressBar jProgressBar;
        protected javax.swing.JLabel labelJLabel;
        protected javax.swing.JLabel messageJLabel;
        // End of variables declaration//GEN-END:variables
    
}
