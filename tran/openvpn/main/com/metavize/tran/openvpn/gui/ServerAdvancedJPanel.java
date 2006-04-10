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

package com.metavize.tran.openvpn.gui;

import com.metavize.gui.widgets.dialogs.MConfigJDialog;
import com.metavize.gui.transform.*;
import com.metavize.gui.util.*;

import com.metavize.mvvm.security.*;
import com.metavize.mvvm.*;
import com.metavize.mvvm.networking.*;
import com.metavize.mvvm.tran.*;

import com.metavize.tran.openvpn.*;

import java.awt.*;
import javax.swing.JDialog;
import javax.swing.SpinnerNumberModel;
import javax.swing.JSpinner;

public class ServerAdvancedJPanel extends javax.swing.JPanel
    implements Savable<Object>, Refreshable<Object> {

    // SETTINGS CHANGE NOTIFICATION /////////
    private SettingsChangedListener settingsChangedListener;
    public void setSettingsChangedListener(SettingsChangedListener settingsChangedListener){
	this.settingsChangedListener = settingsChangedListener;
    }
    ///////////////////////////////////////////
  
    public ServerAdvancedJPanel() {
        initComponents();
	    Util.setPortView(portJSpinner, VpnSettings.DEFAULT_PUBLIC_PORT);
		defaultJLabel.setText("(default: " + VpnSettings.DEFAULT_PUBLIC_PORT + ")");
    }

    public void doSave(Object settings, boolean validateOnly) throws Exception {

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
	    VpnSettings vpnSettings = (VpnSettings) settings;
		vpnSettings.setPublicPort( port );
    }
	}
    int portCurrent;

    public void doRefresh(Object settings){
		VpnSettings vpnSettings = (VpnSettings) settings;
	
        // PORT //
        portCurrent = vpnSettings.getPublicPort();
        portJSpinner.setValue(portCurrent);
	((JSpinner.DefaultEditor)portJSpinner.getEditor()).getTextField().setText(Integer.toString(portCurrent));
	((JSpinner.DefaultEditor)portJSpinner.getEditor()).getTextField().setBackground(Color.WHITE);
    }
    
    
        private void initComponents() {//GEN-BEGIN:initComponents
                java.awt.GridBagConstraints gridBagConstraints;

                dnsButtonGroup = new javax.swing.ButtonGroup();
                portJPanel = new javax.swing.JPanel();
                jLabel9 = new javax.swing.JLabel();
                staticIPJPanel = new javax.swing.JPanel();
                portJLabel = new javax.swing.JLabel();
                portJSpinner = new javax.swing.JSpinner();
                defaultJLabel = new javax.swing.JLabel();

                setLayout(new java.awt.GridBagLayout());

                setMaximumSize(new java.awt.Dimension(550, 250));
                setMinimumSize(new java.awt.Dimension(550, 250));
                setPreferredSize(new java.awt.Dimension(550, 250));
                portJPanel.setLayout(new java.awt.GridBagLayout());

                portJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Server Port", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
                jLabel9.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel9.setText("<html> The Server Port is the port that the VPN Server will accept connections from.  This setting is intended to allow you to run your VPN Server from a non-standard port in case you already have a VPN running, or you have a complex network setup.</html>");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 0);
                portJPanel.add(jLabel9, gridBagConstraints);

                staticIPJPanel.setLayout(new java.awt.GridBagLayout());

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
                portJSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
                        public void stateChanged(javax.swing.event.ChangeEvent evt) {
                                portJSpinnerStateChanged(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
                staticIPJPanel.add(portJSpinner, gridBagConstraints);

                defaultJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 2;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
                staticIPJPanel.add(defaultJLabel, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.ipadx = 150;
                gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
                portJPanel.add(staticIPJPanel, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
                add(portJPanel, gridBagConstraints);

        }//GEN-END:initComponents

		private void portJSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_portJSpinnerStateChanged
		if( !portJSpinner.getValue().equals(portCurrent) && (settingsChangedListener != null) )
		    settingsChangedListener.settingsChanged(this);				// TODO add your handling code here:
		}//GEN-LAST:event_portJSpinnerStateChanged
                                

    
        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JLabel defaultJLabel;
        private javax.swing.ButtonGroup dnsButtonGroup;
        private javax.swing.JLabel jLabel9;
        private javax.swing.JLabel portJLabel;
        private javax.swing.JPanel portJPanel;
        private javax.swing.JSpinner portJSpinner;
        private javax.swing.JPanel staticIPJPanel;
        // End of variables declaration//GEN-END:variables
    

}
