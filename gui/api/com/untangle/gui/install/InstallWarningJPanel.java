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
