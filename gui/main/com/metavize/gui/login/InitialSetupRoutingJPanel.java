/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.gui.login;

import com.metavize.mvvm.tran.IPaddr;

import com.metavize.gui.widgets.wizard.*;
import com.metavize.gui.util.Util;
import javax.swing.SwingUtilities;
import java.awt.Color;

public class InitialSetupRoutingJPanel extends MWizardPageJPanel {
    
		
    public InitialSetupRoutingJPanel() {
        initComponents();
        

    }

    public void initialFocus(){
	natEnabledJRadioButton.requestFocus();
    }

    
    public static boolean getNatEnabled(){ return natEnabled; }
    private static boolean natEnabled;
    public static IPaddr getAddress(){ return natAddress; }
    private static IPaddr natAddress;
    public static IPaddr getNetmask(){ return natNetmask; }
    private static IPaddr natNetmask;

    private Exception exception;
    public void doSave(Object settings, boolean validateOnly) throws Exception {
        
	exception = null;
        SwingUtilities.invokeAndWait( new Runnable(){ public void run(){

	    addressJTextField.setBackground( Color.WHITE );
	    netmaskJTextField.setBackground( Color.WHITE );

	    // NAT ENABLED
	    natEnabled = natEnabledJRadioButton.isSelected();
	    if( natEnabled ){

		// ADDRESS
		try{ natAddress = IPaddr.parse( addressJTextField.getText() ); }
		catch(Exception e){ 
		    addressJTextField.setBackground(Util.INVALID_BACKGROUND_COLOR);
		    exception = e;
		    return;
		}

		// NETMASK
		try{ natNetmask = IPaddr.parse( netmaskJTextField.getText() ); }
		catch(Exception e){ 
		    netmaskJTextField.setBackground(Util.INVALID_BACKGROUND_COLOR);
		    exception = e;
		    return;
		}

	    }
	}});

	if( exception != null )
	    throw exception;
    }
    

        private void initComponents() {//GEN-BEGIN:initComponents
                java.awt.GridBagConstraints gridBagConstraints;

                natButtonGroup = new javax.swing.ButtonGroup();
                jLabel2 = new javax.swing.JLabel();
                jLabel3 = new javax.swing.JLabel();
                natEnabledJRadioButton = new javax.swing.JRadioButton();
                natDisabledJRadioButton = new javax.swing.JRadioButton();
                staticIPJPanel = new javax.swing.JPanel();
                addressJLabel = new javax.swing.JLabel();
                addressJTextField = new javax.swing.JTextField();
                netmaskJLabel = new javax.swing.JLabel();
                netmaskJTextField = new javax.swing.JTextField();

                setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

                setOpaque(false);
                jLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel2.setText("<html>How would you like to use your EdgeGuard?</html>");
                add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 30, 375, -1));

                jLabel3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/metavize/gui/login/ProductShot.png")));
                add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(-130, 230, -1, -1));

                natButtonGroup.add(natEnabledJRadioButton);
                natEnabledJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
                natEnabledJRadioButton.setSelected(true);
                natEnabledJRadioButton.setText("<html><b>Router</b> (NAT Enabled) This is the default setting.</html>");
                natEnabledJRadioButton.setOpaque(false);
                natEnabledJRadioButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                natEnabledJRadioButtonActionPerformed(evt);
                        }
                });

                add(natEnabledJRadioButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 60, -1, -1));

                natButtonGroup.add(natDisabledJRadioButton);
                natDisabledJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
                natDisabledJRadioButton.setText("<html><b>Bridge</b> (NAT Disabled)</html>");
                natDisabledJRadioButton.setOpaque(false);
                natDisabledJRadioButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                natDisabledJRadioButtonActionPerformed(evt);
                        }
                });

                add(natDisabledJRadioButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 160, -1, -1));

                staticIPJPanel.setLayout(new java.awt.GridBagLayout());

                staticIPJPanel.setOpaque(false);
                addressJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                addressJLabel.setText("IP Address:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                staticIPJPanel.add(addressJLabel, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
                staticIPJPanel.add(addressJTextField, gridBagConstraints);

                netmaskJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                netmaskJLabel.setText("Netmask:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                staticIPJPanel.add(netmaskJLabel, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
                staticIPJPanel.add(netmaskJTextField, gridBagConstraints);

                add(staticIPJPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(120, 80, 240, 60));

        }//GEN-END:initComponents

    private void natDisabledJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_natDisabledJRadioButtonActionPerformed
	natEnabledDependency(false);
    }//GEN-LAST:event_natDisabledJRadioButtonActionPerformed
    
    private void natEnabledJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_natEnabledJRadioButtonActionPerformed
	natEnabledDependency(true);
    }//GEN-LAST:event_natEnabledJRadioButtonActionPerformed


    private void natEnabledDependency(boolean enabled){
	addressJTextField.setEnabled(enabled);
	addressJLabel.setEnabled(enabled);
	netmaskJTextField.setEnabled(enabled);
	netmaskJLabel.setEnabled(enabled);
    }
    
        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JLabel addressJLabel;
        public javax.swing.JTextField addressJTextField;
        private javax.swing.JLabel jLabel2;
        private javax.swing.JLabel jLabel3;
        private javax.swing.ButtonGroup natButtonGroup;
        private javax.swing.JRadioButton natDisabledJRadioButton;
        private javax.swing.JRadioButton natEnabledJRadioButton;
        private javax.swing.JLabel netmaskJLabel;
        public javax.swing.JTextField netmaskJTextField;
        private javax.swing.JPanel staticIPJPanel;
        // End of variables declaration//GEN-END:variables
    
}
