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

import com.metavize.gui.widgets.wizard.*;
import java.util.Arrays;
import com.metavize.mvvm.tran.IPaddr;
import com.metavize.gui.util.Util;
import com.metavize.gui.widgets.dialogs.*;
import com.metavize.mvvm.NetworkingManager;
import com.metavize.mvvm.NetworkingConfiguration;

import javax.swing.*;
import java.awt.*;

public class InitialSetupNetworkJPanel extends MWizardPageJPanel {

    private static final String EXCEPTION_DHCP_IP_ADDRESS = "You have manually specified an invalid \"IP Address\".  Please correct this before proceeding.";
    private static final String EXCEPTION_DHCP_NETMASK = "You have manually specified an invalid \"Netmask\".  Please correct this before proceeding.";
    private static final String EXCEPTION_DHCP_GATEWAY = "You have manually specified an invalid \"Default Route\".  Please correct this before proceeding.";
    private static final String EXCEPTION_DHCP_DNS_1 = "You have manually specified an invalid \"Primary DNS\".  Please correct this before proceeding.";
    private static final String EXCEPTION_DHCP_DNS_2 = "You have manually specified an invalid \"Secondary DNS\".  Please correct this before proceeding.";
    private static final String EXCEPTION_HOSTNAME = "You must specify a dotted hostname for your EdgeGuard.  Please correct this before proceeding.";
    private static final String EMPTY_DNS2 = "";

    public InitialSetupNetworkJPanel() {
        initComponents();
        setDhcpEnabledDependency(dhcpEnabledRadioButton.isSelected());
    }

    public void initialFocus(){
	hostnameJTextField.requestFocus();
    }
	
    boolean isDhcpEnabled;
    String hostString;
    IPaddr host;
    String netmaskString;
    IPaddr netmask;
    String gatewayString;
    IPaddr gateway;
    String dns1String;
    IPaddr dns1;
    String dns2String;
    IPaddr dns2;
    String hostnameString;
    Exception exception;
    
    MProgressJDialog mProgressJDialog;
    JProgressBar jProgressBar;

    public void doSave(Object settings, boolean validateOnly) throws Exception {

        SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
	    dhcpIPaddrJTextField.setBackground( Color.WHITE );
	    dhcpNetmaskJTextField.setBackground( Color.WHITE );
	    dhcpRouteJTextField.setBackground( Color.WHITE );
	    dnsPrimaryJTextField.setBackground( Color.WHITE );
	    dnsSecondaryJTextField.setBackground( Color.WHITE );
	    hostnameJTextField.setBackground( Color.WHITE );

	    isDhcpEnabled = dhcpEnabledRadioButton.isSelected();
	    hostString = dhcpIPaddrJTextField.getText();
	    netmaskString = dhcpNetmaskJTextField.getText();
	    gatewayString = dhcpRouteJTextField.getText();
	    dns1String = dnsPrimaryJTextField.getText();
	    dns2String = dnsSecondaryJTextField.getText();
	    hostnameString = hostnameJTextField.getText();

	    exception = null;
	    
	    if( !isDhcpEnabled ){
		try{
		    host = IPaddr.parse( hostString );
		    if( host.isEmpty() )
			throw new Exception();
		}
		catch(Exception e){
		    dhcpIPaddrJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
		    exception = new Exception(EXCEPTION_DHCP_IP_ADDRESS);
		    return;
		}
	    }
	    
	    if( !isDhcpEnabled ){
		try{
		    netmask = IPaddr.parse( netmaskString );
		    if( netmask.isEmpty() )
			throw new Exception();
		} 
		catch(Exception e){
		    dhcpNetmaskJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
		    exception = new Exception(EXCEPTION_DHCP_NETMASK);
		    return;
		}
	    }
	    
	    if( !isDhcpEnabled ){
		try{
		    gateway = IPaddr.parse( gatewayString );
		    if( gateway.isEmpty() )
			throw new Exception();
		}
		catch(Exception e){
		    dhcpRouteJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
		    exception = new Exception(EXCEPTION_DHCP_GATEWAY);
		    return;
		}
	    }
	    
	    if( !isDhcpEnabled ){
		try{
		    dns1 = IPaddr.parse( dns1String );
		    if( dns1.isEmpty() )
			throw new Exception();
		}
		catch(Exception e){
		    dnsPrimaryJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
		    exception = new Exception(EXCEPTION_DHCP_DNS_1);
		    return;
		}
	    }
	    
	    if( !isDhcpEnabled ){
		try{
		    if ( dns2String.length() > 0 ) {
			dns2 = IPaddr.parse( dns2String );
		    } else {
			/* Ignoring empty secondary DNS entry, dns2 = null is okay for network settings */
		    }
		}
		catch(Exception e){
		    dnsSecondaryJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
		    exception = new Exception(EXCEPTION_DHCP_DNS_2);
		    return;
		}
	    }
		
		try{
		    if( hostnameString.length() == 0 )
			throw new Exception();
		}
		catch(Exception e){
		    hostnameJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
		    exception = new Exception(EXCEPTION_HOSTNAME);
		    return;
		}
	    
	}});

        if( exception != null )
	    throw exception;

	// SAVE SETTINGS ////////////
	if( !validateOnly ){
	    InitialSetupWizard.getInfiniteProgressJComponent().startLater("Saving Network Settings... (This may take up to one minute)");
            try{
                NetworkingConfiguration networkingConfiguration = Util.getNetworkingManager().get();
                networkingConfiguration.isDhcpEnabled( isDhcpEnabled );
                if( !isDhcpEnabled ){
                    networkingConfiguration.host( host );
                    networkingConfiguration.netmask( netmask );
                    networkingConfiguration.gateway( gateway );
                    networkingConfiguration.dns1( dns1 );
                    networkingConfiguration.dns2( dns2 );
                }
		networkingConfiguration.hostname( hostnameString );
		InitialSetupWizard.setSharedData( hostnameString );
                Util.getNetworkingManager().set(networkingConfiguration);
		InitialSetupWizard.getInfiniteProgressJComponent().stopLater(1500l);
            }
            catch(Exception e){
		InitialSetupWizard.getInfiniteProgressJComponent().stopLater(-1l);
                throw e;
            }
        }
        
    }
    

        private void initComponents() {//GEN-BEGIN:initComponents
                java.awt.GridBagConstraints gridBagConstraints;

                buttonGroup1 = new javax.swing.ButtonGroup();
                jLabel2 = new javax.swing.JLabel();
                dhcpEnabledRadioButton = new javax.swing.JRadioButton();
                dhcpDisabledRadioButton = new javax.swing.JRadioButton();
                staticIPJPanel = new javax.swing.JPanel();
                jLabel1 = new javax.swing.JLabel();
                dhcpIPaddrJTextField = new javax.swing.JTextField();
                jLabel7 = new javax.swing.JLabel();
                dhcpNetmaskJTextField = new javax.swing.JTextField();
                jLabel5 = new javax.swing.JLabel();
                dhcpRouteJTextField = new javax.swing.JTextField();
                jLabel6 = new javax.swing.JLabel();
                dnsPrimaryJTextField = new javax.swing.JTextField();
                jLabel8 = new javax.swing.JLabel();
                dnsSecondaryJTextField = new javax.swing.JTextField();
                jLabel9 = new javax.swing.JLabel();
                jSeparator1 = new javax.swing.JSeparator();
                jLabel4 = new javax.swing.JLabel();
                jLabel3 = new javax.swing.JLabel();
                hostnameJPanel = new javax.swing.JPanel();
                jLabel10 = new javax.swing.JLabel();
                hostnameJTextField = new javax.swing.JTextField();

                setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

                setOpaque(false);
                jLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel2.setText("<html>Please specify how EdgeGuard will get its network settings.</html>");
                add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 120, -1, -1));

                buttonGroup1.add(dhcpEnabledRadioButton);
                dhcpEnabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
                dhcpEnabledRadioButton.setSelected(true);
                dhcpEnabledRadioButton.setText("<html><b>Automatically</b> through DHCP. (Use this if unsure)</html>");
                dhcpEnabledRadioButton.setActionCommand("<html><b>Use DHCP</b> to automatically set EdgeGuard's IP address from the network's DHCP server.</html>");
                dhcpEnabledRadioButton.setOpaque(false);
                dhcpEnabledRadioButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                dhcpEnabledRadioButtonActionPerformed(evt);
                        }
                });

                add(dhcpEnabledRadioButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 140, -1, -1));

                buttonGroup1.add(dhcpDisabledRadioButton);
                dhcpDisabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
                dhcpDisabledRadioButton.setText("<html><b>Manually</b> through the fields below.</html>");
                dhcpDisabledRadioButton.setOpaque(false);
                dhcpDisabledRadioButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                dhcpDisabledRadioButtonActionPerformed(evt);
                        }
                });

                add(dhcpDisabledRadioButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 160, -1, -1));

                staticIPJPanel.setLayout(new java.awt.GridBagLayout());

                staticIPJPanel.setOpaque(false);
                jLabel1.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel1.setText("IP Address:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                staticIPJPanel.add(jLabel1, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
                staticIPJPanel.add(dhcpIPaddrJTextField, gridBagConstraints);

                jLabel7.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel7.setText("Netmask:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                staticIPJPanel.add(jLabel7, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
                staticIPJPanel.add(dhcpNetmaskJTextField, gridBagConstraints);

                jLabel5.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel5.setText("Default Route / Gateway:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 2;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                staticIPJPanel.add(jLabel5, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 2;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
                staticIPJPanel.add(dhcpRouteJTextField, gridBagConstraints);

                jLabel6.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel6.setText("Primary DNS:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 3;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                staticIPJPanel.add(jLabel6, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 3;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
                staticIPJPanel.add(dnsPrimaryJTextField, gridBagConstraints);

                jLabel8.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel8.setText("Secondary DNS:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 4;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                staticIPJPanel.add(jLabel8, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 4;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
                staticIPJPanel.add(dnsSecondaryJTextField, gridBagConstraints);

                jLabel9.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel9.setText(" (Optional)");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 2;
                gridBagConstraints.gridy = 4;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                staticIPJPanel.add(jLabel9, gridBagConstraints);

                add(staticIPJPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 180, 350, 130));

                add(jSeparator1, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 100, 350, -1));

                jLabel4.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel4.setText("<html>Please specify EdgeGuard's hostname on your network.</html>");
                add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 30, -1, -1));

                jLabel3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/metavize/gui/login/ProductShot.png")));
                jLabel3.setEnabled(false);
                add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(-130, 230, -1, -1));

                hostnameJPanel.setLayout(new java.awt.GridBagLayout());

                hostnameJPanel.setOpaque(false);
                jLabel10.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel10.setText("Hostname:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                hostnameJPanel.add(jLabel10, gridBagConstraints);

                hostnameJTextField.setText("edgeguard.somedomain.com");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
                hostnameJPanel.add(hostnameJTextField, gridBagConstraints);

                add(hostnameJPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(65, 50, 325, -1));

        }//GEN-END:initComponents

    private void dhcpDisabledRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dhcpDisabledRadioButtonActionPerformed
        setDhcpEnabledDependency( false );
    }//GEN-LAST:event_dhcpDisabledRadioButtonActionPerformed

    private void dhcpEnabledRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dhcpEnabledRadioButtonActionPerformed
        setDhcpEnabledDependency( true );
    }//GEN-LAST:event_dhcpEnabledRadioButtonActionPerformed
    private void setDhcpEnabledDependency(boolean enabled){
        dhcpIPaddrJTextField.setEnabled( !enabled );
        dhcpNetmaskJTextField.setEnabled( !enabled );
        dhcpRouteJTextField.setEnabled( !enabled );
        dnsPrimaryJTextField.setEnabled( !enabled );
        dnsSecondaryJTextField.setEnabled( !enabled );
    }
    
        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.ButtonGroup buttonGroup1;
        public javax.swing.JRadioButton dhcpDisabledRadioButton;
        public javax.swing.JRadioButton dhcpEnabledRadioButton;
        public javax.swing.JTextField dhcpIPaddrJTextField;
        public javax.swing.JTextField dhcpNetmaskJTextField;
        public javax.swing.JTextField dhcpRouteJTextField;
        public javax.swing.JTextField dnsPrimaryJTextField;
        public javax.swing.JTextField dnsSecondaryJTextField;
        private javax.swing.JPanel hostnameJPanel;
        private javax.swing.JTextField hostnameJTextField;
        private javax.swing.JLabel jLabel1;
        private javax.swing.JLabel jLabel10;
        private javax.swing.JLabel jLabel2;
        private javax.swing.JLabel jLabel3;
        private javax.swing.JLabel jLabel4;
        private javax.swing.JLabel jLabel5;
        private javax.swing.JLabel jLabel6;
        private javax.swing.JLabel jLabel7;
        private javax.swing.JLabel jLabel8;
        private javax.swing.JLabel jLabel9;
        private javax.swing.JSeparator jSeparator1;
        private javax.swing.JPanel staticIPJPanel;
        // End of variables declaration//GEN-END:variables
    
}
