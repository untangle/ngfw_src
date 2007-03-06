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

package com.untangle.gui.configuration;

import com.untangle.gui.widgets.dialogs.MConfigJDialog;
import com.untangle.gui.transform.*;
import com.untangle.gui.util.*;

import com.untangle.mvvm.security.*;
import com.untangle.mvvm.*;
import com.untangle.mvvm.networking.AddressSettings;
import com.untangle.mvvm.tran.*;

import java.awt.*;
import javax.swing.JDialog;
import javax.swing.SpinnerNumberModel;
import javax.swing.JSpinner;

public class RemotePublicAddressJPanel extends javax.swing.JPanel
    implements Savable<RemoteCompoundSettings>, Refreshable<RemoteCompoundSettings> {

    private static final String EXCEPTION_NO_ADDRESS = "You must provide a valid IP Address.";

    public RemotePublicAddressJPanel() {
        initComponents();
        Util.setPortView(portJSpinner, 443);
		Util.addPanelFocus(this, disabledJRadioButton);
        Util.addFocusHighlight(addressJTextField);
        Util.addFocusHighlight(portJSpinner);
    }

    public void doSave(RemoteCompoundSettings remoteCompoundSettings, boolean validateOnly) throws Exception {

        // PUBLIC ADDRESS ENABLED //////////
	boolean isPublicAddressEnabled = enabledJRadioButton.isSelected();
        
        // ADDRESS //
	addressJTextField.setBackground( Color.WHITE );
	IPaddr address = null;
        try{ address = IPaddr.parse(addressJTextField.getText()); }
        catch(Exception e){
            /* Only throw an expception if it is enabled */
            if ( isPublicAddressEnabled ) {
                addressJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
                throw new Exception(EXCEPTION_NO_ADDRESS);
            }
        }

        // PORT //
	((JSpinner.DefaultEditor)portJSpinner.getEditor()).getTextField().setBackground(Color.WHITE);
	int port = 0;
	try{ portJSpinner.commitEdit(); }
	catch(Exception e){ 
	    ((JSpinner.DefaultEditor)portJSpinner.getEditor()).getTextField().setBackground(Util.INVALID_BACKGROUND_COLOR);
	    throw new Exception(Util.EXCEPTION_PORT_RANGE);
	}
        port = (Integer) portJSpinner.getValue();
		
	// SAVE SETTINGS ////////////
	if( !validateOnly ){
	    AddressSettings addressSettings = remoteCompoundSettings.getAddressSettings();
            addressSettings.setIsPublicAddressEnabled( isPublicAddressEnabled );
            
            if ( address != null ) addressSettings.setPublicIPaddr( address );
            if ( 0 < port && port < 0xFFFF ) addressSettings.setPublicPort( port );
        }
    }


    boolean isPublicAddressEnabledCurrent;
    IPaddr addressCurrent;
    int portCurrent;

    public void doRefresh(RemoteCompoundSettings remoteCompoundSettings){
	AddressSettings addressSettings = remoteCompoundSettings.getAddressSettings();
        
	// PUBLIC ADDRESS ENABLED /////
	isPublicAddressEnabledCurrent = addressSettings.getIsPublicAddressEnabled();
        
	setEnabledDependency( isPublicAddressEnabledCurrent );
	if( isPublicAddressEnabledCurrent )
            enabledJRadioButton.setSelected(true);
        else
            disabledJRadioButton.setSelected(true);

        // ADDRESS //
        addressCurrent = addressSettings.getPublicIPaddr();
        if ( addressCurrent == null ) {
            addressJTextField.setText("");
        } else {
            addressJTextField.setText(addressCurrent.toString());
        }
        addressJTextField.setBackground(Color.WHITE);
	
        // PORT //
        portCurrent = addressSettings.getPublicPort();
        portJSpinner.setValue(portCurrent);
	((JSpinner.DefaultEditor)portJSpinner.getEditor()).getTextField().setText(Integer.toString(portCurrent));
	((JSpinner.DefaultEditor)portJSpinner.getEditor()).getTextField().setBackground(Color.WHITE);
    }
    
    
        private void initComponents() {//GEN-BEGIN:initComponents
                java.awt.GridBagConstraints gridBagConstraints;

                dnsButtonGroup = new javax.swing.ButtonGroup();
                dhcpJPanel = new javax.swing.JPanel();
                jLabel9 = new javax.swing.JLabel();
                disabledJRadioButton = new javax.swing.JRadioButton();
                enabledJRadioButton = new javax.swing.JRadioButton();
                staticIPJPanel = new javax.swing.JPanel();
                addressJLabel = new javax.swing.JLabel();
                addressJTextField = new javax.swing.JTextField();
                portJLabel = new javax.swing.JLabel();
                portJSpinner = new javax.swing.JSpinner();

                setLayout(new java.awt.GridBagLayout());

                setMaximumSize(new java.awt.Dimension(550, 250));
                setMinimumSize(new java.awt.Dimension(550, 250));
                setPreferredSize(new java.awt.Dimension(550, 250));
                dhcpJPanel.setLayout(new java.awt.GridBagLayout());

                dhcpJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Public Address", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
                jLabel9.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel9.setText("<html> The Public Address must be set when the Untangle Server is placed behind a Router on your network (it is not at the \"edge\" of your network).  This is the IP Address and port that will direct traffic to the Untangle Server.  This will also be used in emails.</html>");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 0);
                dhcpJPanel.add(jLabel9, gridBagConstraints);

                dnsButtonGroup.add(disabledJRadioButton);
                disabledJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
                disabledJRadioButton.setText("<html><b>Disabled</b></html>");
                disabledJRadioButton.setActionCommand("<html><b>Use DHCP</b> to automatically set Untangle's IP address from the network's DHCP server.</html>");
                disabledJRadioButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                disabledJRadioButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                dhcpJPanel.add(disabledJRadioButton, gridBagConstraints);

                dnsButtonGroup.add(enabledJRadioButton);
                enabledJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
                enabledJRadioButton.setText("<html><b>Enabled</b></html>");
                enabledJRadioButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                enabledJRadioButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                dhcpJPanel.add(enabledJRadioButton, gridBagConstraints);

                staticIPJPanel.setLayout(new java.awt.GridBagLayout());

                addressJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                addressJLabel.setText("IP Address: ");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 0);
                staticIPJPanel.add(addressJLabel, gridBagConstraints);

                addressJTextField.setMaximumSize(null);
                addressJTextField.setMinimumSize(null);
                addressJTextField.setPreferredSize(null);
                addressJTextField.addCaretListener(new javax.swing.event.CaretListener() {
                        public void caretUpdate(javax.swing.event.CaretEvent evt) {
                                addressJTextFieldCaretUpdate(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 0);
                staticIPJPanel.add(addressJTextField, gridBagConstraints);

                portJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                portJLabel.setText("Port: ");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                staticIPJPanel.add(portJLabel, gridBagConstraints);

                portJSpinner.setFont(new java.awt.Font("Dialog", 0, 12));
                portJSpinner.setMaximumSize(new java.awt.Dimension(75, 19));
                portJSpinner.setMinimumSize(new java.awt.Dimension(75, 19));
                portJSpinner.setPreferredSize(new java.awt.Dimension(75, 19));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
                staticIPJPanel.add(portJSpinner, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.ipadx = 150;
                gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
                dhcpJPanel.add(staticIPJPanel, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
                add(dhcpJPanel, gridBagConstraints);

        }//GEN-END:initComponents

		private void addressJTextFieldCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_addressJTextFieldCaretUpdate
				// TODO add your handling code here:
		}//GEN-LAST:event_addressJTextFieldCaretUpdate
                        
	private void enabledJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enabledJRadioButtonActionPerformed
	    setEnabledDependency( true );
	}//GEN-LAST:event_enabledJRadioButtonActionPerformed
    
	private void disabledJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_disabledJRadioButtonActionPerformed
	    setEnabledDependency( false );
	}//GEN-LAST:event_disabledJRadioButtonActionPerformed
    
    private void setEnabledDependency(boolean enabled){
	addressJLabel.setEnabled(enabled);
	portJLabel.setEnabled(enabled);
	addressJTextField.setEnabled(enabled);
	portJSpinner.setEnabled(enabled);
    }

    
        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JLabel addressJLabel;
        public javax.swing.JTextField addressJTextField;
        private javax.swing.JPanel dhcpJPanel;
        public javax.swing.JRadioButton disabledJRadioButton;
        private javax.swing.ButtonGroup dnsButtonGroup;
        public javax.swing.JRadioButton enabledJRadioButton;
        private javax.swing.JLabel jLabel9;
        private javax.swing.JLabel portJLabel;
        private javax.swing.JSpinner portJSpinner;
        private javax.swing.JPanel staticIPJPanel;
        // End of variables declaration//GEN-END:variables
    

}
