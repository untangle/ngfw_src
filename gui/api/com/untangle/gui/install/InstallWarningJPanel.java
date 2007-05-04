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

package com.untangle.gui.install;

import java.awt.Window;

import com.untangle.gui.util.*;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.widgets.wizard.*;

public class InstallWarningJPanel extends MWizardPageJPanel {


    public InstallWarningJPanel() {
        initComponents();
    }

    protected boolean leavingForwards(){
        MTwoButtonJDialog dialog = MTwoButtonJDialog.factory((Window) this.getTopLevelAncestor(), "Install Wizard",
                                                             "Your selected disk will be erased"
                                                             + " if you continue installation.  Would you like to continue?",
                                                             "Install Wizard Warning",
                                                             "Warning");
        dialog.setProceedText("<html><b>Continue</b></html>");
        dialog.setCancelText("<html><b>Don't Continue</b></html>");
        dialog.setVisible(true);
        if( dialog.isProceeding() )
            return true;
        else
            return false;
    }

    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        contentJPanel = new javax.swing.JPanel();
        headerJLabel = new javax.swing.JLabel();
        subheaderJLabel = new javax.swing.JLabel();
        messageJLabel = new javax.swing.JLabel();
        backgroundJPabel = new javax.swing.JLabel();

        setLayout(new java.awt.GridBagLayout());

        setOpaque(false);
        contentJPanel.setLayout(new java.awt.GridBagLayout());

        contentJPanel.setOpaque(false);
        headerJLabel.setFont(new java.awt.Font("Dialog", 1, 18));
        headerJLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        headerJLabel.setText("<html><font color=\"#FF0000\">Warning!</font></html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(15, 15, 0, 15);
        contentJPanel.add(headerJLabel, gridBagConstraints);

        subheaderJLabel.setFont(new java.awt.Font("Dialog", 0, 18));
        subheaderJLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        subheaderJLabel.setText("<html><font color=\"#FF0000\">If you continue, in preparation for the Untangle Platform installation process, YOUR DISK WILL BE ERASED AND ALL ITS DATA WILL BE LOST!</font></html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(15, 15, 0, 15);
        contentJPanel.add(subheaderJLabel, gridBagConstraints);

        messageJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        messageJLabel.setText("<html>If you would not like to continue, press the Close Window button.  Otherwise, press the Finish button.</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(15, 15, 0, 15);
        contentJPanel.add(messageJLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(contentJPanel, gridBagConstraints);

        backgroundJPabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/install/ProductShot.png")));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
        gridBagConstraints.weightx = 1.0;
        add(backgroundJPabel, gridBagConstraints);

    }//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel backgroundJPabel;
    private javax.swing.JPanel contentJPanel;
    private javax.swing.JLabel headerJLabel;
    private javax.swing.JLabel messageJLabel;
    private javax.swing.JLabel subheaderJLabel;
    // End of variables declaration//GEN-END:variables

}
