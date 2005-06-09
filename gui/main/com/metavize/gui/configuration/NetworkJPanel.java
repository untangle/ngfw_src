/*
 * NetworkJPanel.java
 *
 * Created on February 22, 2005, 1:10 PM
 */

package com.metavize.gui.configuration;

import com.metavize.gui.transform.*;
import com.metavize.gui.util.*;

import com.metavize.mvvm.security.*;
import com.metavize.mvvm.*;
import com.metavize.mvvm.tran.*;

import java.awt.*;

/**
 *
 * @author  inieves
 */
public class NetworkJPanel extends javax.swing.JPanel implements Savable, Refreshable {

    private static final String EXCEPTION_DHCP_IP_ADDRESS = "Invalid DHCP \"IP Address\" specified.";
    private static final String EXCEPTION_DHCP_NETMASK = "Invalid DHCP \"Netmask\" specified.";
    private static final String EXCEPTION_DHCP_GATEWAY = "Invalid DHCP \"Default Route\" specified.";
    private static final String EXCEPTION_DHCP_DNS_1 = "Invalid DHCP \"Primary DNS\" specified.";
    private static final String EXCEPTION_DHCP_DNS_2 = "Invalid DHCP \"Secondary DNS\" specified.";

    
    public NetworkJPanel() {
        initComponents();
    }

    public void doSave(Object settings, boolean validateOnly) throws Exception {

        // DHCP ENABLED //////////
	boolean isDhcpEnabled = dhcpEnabledRadioButton.isSelected();

	// DHCP HOST ////////////
	IPaddr host = null;
        dhcpIPaddrJTextField.setBackground( Color.WHITE );
        if( !isDhcpEnabled ){
            try{
                host = IPaddr.parse( dhcpIPaddrJTextField.getText() );
                if( host.isEmpty() )
                    throw new Exception();
            }
            catch(Exception e){
                dhcpIPaddrJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
		throw new Exception(EXCEPTION_DHCP_IP_ADDRESS);
            }
	}

	// DHCP NETMASK /////////
	IPaddr netmask = null;
        dhcpNetmaskJTextField.setBackground( Color.WHITE );
	if( !isDhcpEnabled ){
            try{
                netmask = IPaddr.parse( dhcpNetmaskJTextField.getText() );
                if( netmask.isEmpty() )
                    throw new Exception();
            } 
            catch(Exception e){
                dhcpNetmaskJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
		throw new Exception(EXCEPTION_DHCP_NETMASK);
            }
	}

	// DHCP GATEWAY /////////
	IPaddr gateway = null;
        dhcpRouteJTextField.setBackground( Color.WHITE );
	if( !isDhcpEnabled ){
            try{
                gateway = IPaddr.parse( dhcpRouteJTextField.getText() );
                if( gateway.isEmpty() )
                    throw new Exception();
            }
            catch(Exception e){
                dhcpRouteJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
		throw new Exception(EXCEPTION_DHCP_GATEWAY);
            }
	}

	// DHCP DNS1 ///////////
	IPaddr dns1 = null;
        dnsPrimaryJTextField.setBackground( Color.WHITE );
	if( !isDhcpEnabled ){
            try{
                dns1 = IPaddr.parse( dnsPrimaryJTextField.getText() );
                if( dns1.isEmpty() )
                    throw new Exception();
            }
            catch(Exception e){
                dnsPrimaryJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
		throw new Exception(EXCEPTION_DHCP_DNS_1);
            }
	}

	// DHCP DNS2 ///////
	IPaddr dns2 = null;
        dnsSecondaryJTextField.setBackground( Color.WHITE );
	if( !isDhcpEnabled ){
            try{
                dns2 = IPaddr.parse( dnsSecondaryJTextField.getText() );
            }
            catch(Exception e){
                dnsSecondaryJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
		throw new Exception(EXCEPTION_DHCP_DNS_2);
            }
        }

	// SAVE SETTINGS ////////////
	if( !validateOnly ){
	    NetworkingConfiguration networkingConfiguration = (NetworkingConfiguration) settings;
	    networkingConfiguration.isDhcpEnabled( isDhcpEnabled );
	    if( !isDhcpEnabled ){
		networkingConfiguration.host( host );
		networkingConfiguration.netmask( netmask );
		networkingConfiguration.gateway( gateway );
		networkingConfiguration.dns1( dns1 );
		networkingConfiguration.dns2( dns2 );
	    }
        }

    }

    public void doRefresh(Object settings){
        NetworkingConfiguration networkingConfiguration = (NetworkingConfiguration) settings;
        
	// DHCP ENABLED /////
	boolean isDhcpEnabled = networkingConfiguration.isDhcpEnabled();
	setDhcpEnabledDependency( isDhcpEnabled );
        renewDhcpLeaseJButton.setEnabled( isDhcpEnabled );
	if( isDhcpEnabled )
            dhcpEnabledRadioButton.setSelected(true);
        else
            dhcpDisabledRadioButton.setSelected(true);
        
	// DHCP HOST ////
	dhcpIPaddrJTextField.setText( networkingConfiguration.host().toString() );
	dhcpIPaddrJTextField.setBackground( Color.WHITE );
	
	// DHCP NETMASK /////
        dhcpNetmaskJTextField.setText( networkingConfiguration.netmask().toString() );
	dhcpNetmaskJTextField.setBackground( Color.WHITE );

	// DHCP DEFAULT ROUTE ////////
        dhcpRouteJTextField.setText( networkingConfiguration.gateway().toString() );
	dhcpRouteJTextField.setBackground( Color.WHITE );

	// DNS1 ///////////
        dnsPrimaryJTextField.setText( networkingConfiguration.dns1().toString() );
	dnsPrimaryJTextField.setBackground( Color.WHITE );

	// DNS2 //////////
        dnsSecondaryJTextField.setText( networkingConfiguration.dns2().toString() );
	dnsSecondaryJTextField.setBackground( Color.WHITE );
        	
    }
    
    
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        dhcpButtonGroup = new javax.swing.ButtonGroup();
        tcpWindowButtonGroup = new javax.swing.ButtonGroup();
        externalAdminButtonGroup = new javax.swing.ButtonGroup();
        internalAdminButtonGroup = new javax.swing.ButtonGroup();
        restrictAdminButtonGroup = new javax.swing.ButtonGroup();
        sshButtonGroup = new javax.swing.ButtonGroup();
        dhcpJPanel = new javax.swing.JPanel();
        dhcpEnabledRadioButton = new javax.swing.JRadioButton();
        dhcpDisabledRadioButton = new javax.swing.JRadioButton();
        staticIPJPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        dhcpIPaddrJTextField = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        dhcpNetmaskJTextField = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        dhcpRouteJTextField = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        dnsPrimaryJTextField = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        dnsSecondaryJTextField = new javax.swing.JTextField();
        jSeparator2 = new javax.swing.JSeparator();
        jLabel9 = new javax.swing.JLabel();
        renewDhcpLeaseJButton = new javax.swing.JButton();

        setLayout(new java.awt.GridBagLayout());

        setMaximumSize(new java.awt.Dimension(563, 330));
        setMinimumSize(new java.awt.Dimension(563, 330));
        setPreferredSize(new java.awt.Dimension(563, 330));
        dhcpJPanel.setLayout(new java.awt.GridBagLayout());

        dhcpJPanel.setBorder(new javax.swing.border.TitledBorder(null, "External IP Settings", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
        dhcpButtonGroup.add(dhcpEnabledRadioButton);
        dhcpEnabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        dhcpEnabledRadioButton.setText("<html><b>Use DHCP</b> to automatically set EdgeGuard's external IP settings from the network's DHCP server.  The settings are shown in the fields below.</html>");
        dhcpEnabledRadioButton.setActionCommand("<html><b>Use DHCP</b> to automatically set EdgeGuard's IP address from the network's DHCP server.</html>");
        dhcpEnabledRadioButton.setFocusPainted(false);
        dhcpEnabledRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dhcpEnabledRadioButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        dhcpJPanel.add(dhcpEnabledRadioButton, gridBagConstraints);

        dhcpButtonGroup.add(dhcpDisabledRadioButton);
        dhcpDisabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        dhcpDisabledRadioButton.setText("<html><b>Manually Specify</b> EdgeGuard's external IP settings through the fields below.</html>");
        dhcpDisabledRadioButton.setFocusPainted(false);
        dhcpDisabledRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dhcpDisabledRadioButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        dhcpJPanel.add(dhcpDisabledRadioButton, gridBagConstraints);

        staticIPJPanel.setLayout(new java.awt.GridBagLayout());

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

        jLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel2.setText("Default Route:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        staticIPJPanel.add(jLabel2, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        staticIPJPanel.add(dhcpRouteJTextField, gridBagConstraints);

        jLabel3.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel3.setText("Primary DNS:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        staticIPJPanel.add(jLabel3, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        staticIPJPanel.add(dnsPrimaryJTextField, gridBagConstraints);

        jLabel4.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel4.setText("Secondary DNS:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        staticIPJPanel.add(jLabel4, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        staticIPJPanel.add(dnsSecondaryJTextField, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.ipadx = 150;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
        dhcpJPanel.add(staticIPJPanel, gridBagConstraints);

        jSeparator2.setForeground(new java.awt.Color(200, 200, 200));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        dhcpJPanel.add(jSeparator2, gridBagConstraints);

        jLabel9.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel9.setText("<html><b>Renew DHCP Lease</b> tells EdgeGuard to request new IP settings from the DHCP server.  This button is enabled only if DHCP is currently being used.</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 0);
        dhcpJPanel.add(jLabel9, gridBagConstraints);

        renewDhcpLeaseJButton.setFont(new java.awt.Font("Dialog", 0, 12));
        renewDhcpLeaseJButton.setText("Renew DHCP Lease");
        renewDhcpLeaseJButton.setFocusPainted(false);
        renewDhcpLeaseJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                renewDhcpLeaseJButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        dhcpJPanel.add(renewDhcpLeaseJButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
        add(dhcpJPanel, gridBagConstraints);

    }//GEN-END:initComponents

    private void renewDhcpLeaseJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_renewDhcpLeaseJButtonActionPerformed
        DhcpLeaseRenewDialog dhcpLeaseRenewDialog = new DhcpLeaseRenewDialog();
        NetworkingConfiguration newNetworkingConfiguration = dhcpLeaseRenewDialog.getNetworkingConfiguration();
        if( newNetworkingConfiguration != null)
            doRefresh( newNetworkingConfiguration );
    }//GEN-LAST:event_renewDhcpLeaseJButtonActionPerformed

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
    private javax.swing.ButtonGroup dhcpButtonGroup;
    public javax.swing.JRadioButton dhcpDisabledRadioButton;
    public javax.swing.JRadioButton dhcpEnabledRadioButton;
    public javax.swing.JTextField dhcpIPaddrJTextField;
    private javax.swing.JPanel dhcpJPanel;
    public javax.swing.JTextField dhcpNetmaskJTextField;
    public javax.swing.JTextField dhcpRouteJTextField;
    public javax.swing.JTextField dnsPrimaryJTextField;
    public javax.swing.JTextField dnsSecondaryJTextField;
    private javax.swing.ButtonGroup externalAdminButtonGroup;
    private javax.swing.ButtonGroup internalAdminButtonGroup;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JButton renewDhcpLeaseJButton;
    private javax.swing.ButtonGroup restrictAdminButtonGroup;
    private javax.swing.ButtonGroup sshButtonGroup;
    private javax.swing.JPanel staticIPJPanel;
    private javax.swing.ButtonGroup tcpWindowButtonGroup;
    // End of variables declaration//GEN-END:variables
    

}
