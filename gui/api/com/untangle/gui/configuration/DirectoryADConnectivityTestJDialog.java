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

package com.untangle.gui.configuration;

import java.awt.*;
import javax.swing.*;

import com.untangle.gui.util.Util;
import com.untangle.uvm.*;
import com.untangle.uvm.addrbook.*;

public class DirectoryADConnectivityTestJDialog extends javax.swing.JDialog implements java.awt.event.WindowListener {

    private boolean upgradesAvailable = true;

    public DirectoryADConnectivityTestJDialog(Dialog parentDialog){
        super(parentDialog, true);
        init(parentDialog);
    }

    public DirectoryADConnectivityTestJDialog() {
        super(Util.getMMainJFrame(), true);
        init(Util.getMMainJFrame());
    }

    private void init(Window window){
        initComponents();
        setTitle("Active Directory Test");
        this.addWindowListener(this);
        pack();
        this.setBounds( Util.generateCenteredBounds(window.getBounds(), this.getWidth(), this.getHeight()) );
    }

    public boolean upgradesAvailable(){
        return upgradesAvailable;
    }

    public void setVisible(boolean v){
        if(v)
            new ConnectivityCheckThread();
        super.setVisible(v);
        if(!v)
            dispose();
    }

    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        iconJLabel = new javax.swing.JLabel();
        dividerJPanel = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        labelJLabel = new javax.swing.JLabel();
        jProgressBar = new javax.swing.JProgressBar();
        closeJButton = new javax.swing.JButton();
        backgroundJLabel = new com.untangle.gui.widgets.MTiledIconLabel();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Email Test");
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
        labelJLabel.setText("Active Directory Test:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 20, 0);
        jPanel1.add(labelJLabel, gridBagConstraints);

        jProgressBar.setFont(new java.awt.Font("Dialog", 0, 12));
        jProgressBar.setMaximumSize(new java.awt.Dimension(32767, 20));
        jProgressBar.setMinimumSize(new java.awt.Dimension(10, 20));
        jProgressBar.setOpaque(false);
        jProgressBar.setPreferredSize(new java.awt.Dimension(148, 20));
        jProgressBar.setString("");
        jProgressBar.setStringPainted(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 20, 0);
        jPanel1.add(jProgressBar, gridBagConstraints);

        closeJButton.setFont(new java.awt.Font("Default", 0, 12));
        closeJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/IconSave_23x16.png")));
        closeJButton.setText("Close");
        closeJButton.setMargin(new java.awt.Insets(4, 8, 4, 8));
        closeJButton.setMaximumSize(null);
        closeJButton.setMinimumSize(null);
        closeJButton.setOpaque(false);
        closeJButton.setPreferredSize(null);
        closeJButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    closeJButtonActionPerformed(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 20, 0);
        jPanel1.add(closeJButton, gridBagConstraints);

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

    private void closeJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeJButtonActionPerformed
        this.setVisible(false);
    }//GEN-LAST:event_closeJButtonActionPerformed


    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
        this.setVisible(false);
    }


    public void windowActivated(java.awt.event.WindowEvent windowEvent) {}
    public void windowClosed(java.awt.event.WindowEvent windowEvent) {}
    public void windowDeactivated(java.awt.event.WindowEvent windowEvent) {}
    public void windowDeiconified(java.awt.event.WindowEvent windowEvent) {}
    public void windowIconified(java.awt.event.WindowEvent windowEvent) {}
    public void windowOpened(java.awt.event.WindowEvent windowEvent) {}


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel backgroundJLabel;
    private javax.swing.JButton closeJButton;
    private javax.swing.JPanel dividerJPanel;
    private javax.swing.JLabel iconJLabel;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JProgressBar jProgressBar;
    private javax.swing.JLabel labelJLabel;
    // End of variables declaration//GEN-END:variables

    private class ConnectivityCheckThread extends Thread {
        public ConnectivityCheckThread(){
            super("MVCLIENT-ADConnectivityCheckThread");
            this.setDaemon(true);
            this.setContextClassLoader(Util.getClassLoader());
            jProgressBar.setIndeterminate(true);
            jProgressBar.setValue(1);
            jProgressBar.setString("Testing...");
            start();
        }
        public void run() {
            String message;
            String output;
            try{
                message = (Util.getAddressBook().getStatus().isADWorking()?"Success!  Your settings work.":"Failure!  Your settings are not correct.");
                output = Util.getAddressBook().getStatus().adDetail();
            }
            catch(Exception e){
                Util.handleExceptionNoRestart("Error running connectivity tester", e);

                SwingUtilities.invokeLater( new Runnable(){ public void run(){
                    DirectoryADConnectivityTestJDialog.this.jProgressBar.setIndeterminate(false);
                    DirectoryADConnectivityTestJDialog.this.jProgressBar.setValue(1);
                    DirectoryADConnectivityTestJDialog.this.jProgressBar.setString("Warning!  Test failed.  Check your settings.");
                }});
                return;
            }

            try{
                sleep(2000l);
            }
            catch(Exception e){}

            final String finalMessage = message;
            final String finalOutput = output;
            SwingUtilities.invokeLater( new Runnable(){ public void run(){
                DirectoryADConnectivityTestJDialog.this.jProgressBar.setIndeterminate(false);
                DirectoryADConnectivityTestJDialog.this.jProgressBar.setValue(100);
                DirectoryADConnectivityTestJDialog.this.jProgressBar.setString(finalMessage);
                //DirectoryADConnectivityTestJDialog.this.resultJEditorPane.setText(finalOutput);
            }});




        }
    }

}

