/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: MOneButtonJDialog.java 194 2005-04-06 19:13:55Z inieves $
 */

package com.metavize.gui.widgets.dialogs;

import com.metavize.gui.util.Util;
import com.metavize.mvvm.ToolboxManager;


public class MOneButtonJDialog extends javax.swing.JDialog implements java.awt.event.WindowListener {
    
    
    public MOneButtonJDialog() {
	super( (Util.getMMainJFrame()!=null?Util.getMMainJFrame():Util.getMLoginJFrame() ),
	       (Util.getMMainJFrame()!=null?true:false) );
	init();
    }
    
    public MOneButtonJDialog(String applianceName, String warning) {
	super( (Util.getMMainJFrame()!=null?Util.getMMainJFrame():Util.getMLoginJFrame() ),
	       (Util.getMMainJFrame()!=null?true:false) );
	init();
        this.setTitle(applianceName + " Warning");
        messageJLabel.setText("<html><center>" + warning + "</center></html>");
        this.setVisible(true);
    }

    private void init(){
        initComponents();
        this.addWindowListener(this);
        this.setBounds( Util.generateCenteredBounds(Util.getMMainJFrame().getBounds(), this.getWidth(), this.getHeight()) );
    }

    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        proceedJButton = new javax.swing.JButton();
        messageJLabel = new javax.swing.JLabel();
        labelJLabel = new javax.swing.JLabel();
        backgroundJLabel = new com.metavize.gui.widgets.MTiledIconLabel();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Confirm Restart...");
        setModal(true);
        setResizable(false);
        proceedJButton.setFont(new java.awt.Font("Default", 0, 12));
        proceedJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/metavize/gui/images/Button_Close_Dialog_106x17.png")));
        proceedJButton.setDoubleBuffered(true);
        proceedJButton.setFocusPainted(false);
        proceedJButton.setFocusable(false);
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
        messageJLabel.setText("<html><center>\nYou must now shut down the Metavize EdgeGuard Client.<br>\n<br>\nYou can log in again after shutting down, after a brief period.<br>\n</center></html>");
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
        backgroundJLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/metavize/gui/images/LightGreyBackground1600x100.png")));
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
