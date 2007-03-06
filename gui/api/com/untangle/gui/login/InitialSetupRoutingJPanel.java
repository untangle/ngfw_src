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

package com.untangle.gui.login;

import com.untangle.mvvm.tran.IPaddr;
import com.untangle.mvvm.networking.BasicNetworkSettings;
import com.untangle.mvvm.networking.NetworkUtil;

import com.untangle.gui.widgets.wizard.*;
import com.untangle.gui.util.Util;
import javax.swing.SwingUtilities;
import java.awt.Color;

public class InitialSetupRoutingJPanel extends MWizardPageJPanel {
    
    private static final String INIT_ADDRESS = "192.168.1.1";
    private static final String INIT_NETMASK = "255.255.255.0";
		
    public InitialSetupRoutingJPanel() {
        initComponents();
        Util.addFocusHighlight(addressJTextField);
		Util.addFocusHighlight(netmaskJTextField);
    }

    public void initialFocus(){
	natEnabledJRadioButton.requestFocus();
	addressJTextField.setText(INIT_ADDRESS);
	netmaskJTextField.setText(INIT_NETMASK);
	new AutoDetectThread();	
    }
    
    private class AutoDetectThread extends Thread {
	public AutoDetectThread(){
	    setDaemon(true);
	    start();
	}
	public void run(){
	    InitialSetupWizard.getInfiniteProgressJComponent().startLater("Auto-Detecting Usage...");
            try{
                BasicNetworkSettings nc = Util.getNetworkManager().getBasicSettings();
		publicAddress = nc.host();
		final boolean isPrivateNetwork = NetworkUtil.getInstance().isPrivateNetwork(nc.host(),nc.netmask());
		SwingUtilities.invokeLater( new Runnable(){ public void run(){
		    if(isPrivateNetwork){
                        natDisabledJRadioButton.setSelected(true);
			natEnabledDependency(false);
		    }
		    else{
                        natEnabledJRadioButton.setSelected(true);
			natEnabledDependency(true);
		    }
		}});
		
		InitialSetupWizard.getInfiniteProgressJComponent().stopLater(4000l);
	    }
	    catch(Exception e){
		InitialSetupWizard.getInfiniteProgressJComponent().stopLater(-1l);
		Util.handleExceptionNoRestart("Error getting data", e);
	    }
	}
    }
    
    public static boolean getNatEnabled(){ return natEnabled; }
    private static boolean natEnabled = true;
    public static boolean getNatChanged(){ return natChanged; }
    private static boolean natChanged = false;
    public static IPaddr getAddress(){ return natAddress; }
    private static IPaddr natAddress;
    public static IPaddr getNetmask(){ return natNetmask; }
    private static IPaddr natNetmask;
    public static IPaddr getPublicAddress(){ return publicAddress; }
    private static IPaddr publicAddress;

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

		// CHANGED
		if( !addressJTextField.getText().trim().equals(INIT_ADDRESS) )
		    natChanged = true;
		if( !netmaskJTextField.getText().trim().equals(INIT_NETMASK) )
		    natChanged = true;
	    }
	}});

	if( exception != null )
	    throw exception;
    }
    

        private void initComponents() {//GEN-BEGIN:initComponents
                java.awt.GridBagConstraints gridBagConstraints;

                natButtonGroup = new javax.swing.ButtonGroup();
                contentJPanel = new javax.swing.JPanel();
                jLabel2 = new javax.swing.JLabel();
                natEnabledJRadioButton = new javax.swing.JRadioButton();
                questionJLabel = new javax.swing.JLabel();
                staticIPJPanel = new javax.swing.JPanel();
                addressJLabel = new javax.swing.JLabel();
                addressJTextField = new javax.swing.JTextField();
                netmaskJLabel = new javax.swing.JLabel();
                netmaskJTextField = new javax.swing.JTextField();
                natDisabledJRadioButton = new javax.swing.JRadioButton();
                backgroundJPabel = new javax.swing.JLabel();

                setLayout(new java.awt.GridBagLayout());

                setOpaque(false);
                contentJPanel.setLayout(new java.awt.GridBagLayout());

                contentJPanel.setOpaque(false);
                jLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel2.setText("<html>How would you like to use the Untangle Platform?</html>");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(15, 15, 0, 15);
                contentJPanel.add(jLabel2, gridBagConstraints);

                natButtonGroup.add(natEnabledJRadioButton);
                natEnabledJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
                natEnabledJRadioButton.setSelected(true);
                natEnabledJRadioButton.setText("<html><b>Router</b><br>This is recommended if the external ethernet port is connected to an internet connection.  (This enables NAT and DHCP)</html>");
                natEnabledJRadioButton.setOpaque(false);
                natEnabledJRadioButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                natEnabledJRadioButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
                gridBagConstraints.insets = new java.awt.Insets(15, 15, 0, 15);
                contentJPanel.add(natEnabledJRadioButton, gridBagConstraints);

                questionJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                questionJLabel.setText("What is the Untangle Server's address on the internal network?");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
                gridBagConstraints.insets = new java.awt.Insets(5, 50, 0, 25);
                contentJPanel.add(questionJLabel, gridBagConstraints);

                staticIPJPanel.setLayout(new java.awt.GridBagLayout());

                staticIPJPanel.setMinimumSize(new java.awt.Dimension(250, 46));
                staticIPJPanel.setOpaque(false);
                staticIPJPanel.setPreferredSize(new java.awt.Dimension(250, 46));
                addressJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                addressJLabel.setText("IP Address:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                staticIPJPanel.add(addressJLabel, gridBagConstraints);

                addressJTextField.setText("192.168.1.1");
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

                netmaskJTextField.setText("255.255.255.0");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
                staticIPJPanel.add(netmaskJTextField, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.weightx = 1.0;
                contentJPanel.add(staticIPJPanel, gridBagConstraints);

                natButtonGroup.add(natDisabledJRadioButton);
                natDisabledJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
                natDisabledJRadioButton.setText("<html><b>Transparent Bridge</b><br>This is recommended if the external port is connected to a firewall or router. (This disables NAT and DHCP)</html>");
                natDisabledJRadioButton.setOpaque(false);
                natDisabledJRadioButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                natDisabledJRadioButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(20, 15, 0, 15);
                contentJPanel.add(natDisabledJRadioButton, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                add(contentJPanel, gridBagConstraints);

                backgroundJPabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/login/ProductShot.png")));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
                gridBagConstraints.weightx = 1.0;
                add(backgroundJPabel, gridBagConstraints);

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
	questionJLabel.setEnabled(enabled);
    }
    
        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JLabel addressJLabel;
        public javax.swing.JTextField addressJTextField;
        private javax.swing.JLabel backgroundJPabel;
        private javax.swing.JPanel contentJPanel;
        private javax.swing.JLabel jLabel2;
        private javax.swing.ButtonGroup natButtonGroup;
        private javax.swing.JRadioButton natDisabledJRadioButton;
        private javax.swing.JRadioButton natEnabledJRadioButton;
        private javax.swing.JLabel netmaskJLabel;
        public javax.swing.JTextField netmaskJTextField;
        private javax.swing.JLabel questionJLabel;
        private javax.swing.JPanel staticIPJPanel;
        // End of variables declaration//GEN-END:variables
    
}
