/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.nat.gui;

import com.metavize.gui.util.Util;
import com.metavize.gui.transform.*;

import com.metavize.mvvm.tran.*;
import com.metavize.tran.nat.*;
import com.metavize.mvvm.networking.*;

import java.util.List;
import javax.swing.*;


public class SpaceJPanel extends javax.swing.JPanel implements Savable<Object>, Refreshable<Object> {
    
    private static final String EXCEPTION_ALIAS = "The Network Aliases must be a comma separated list of masked IP Addresses.";

    private NetworkSpace networkSpace;

    public SpaceJPanel(NetworkSpace networkSpace) {
	this.networkSpace = networkSpace;
        initComponents();
	mtuJSpinner.setModel(new SpinnerNumberModel(NetworkSpace.DEFAULT_MTU, NetworkSpace.MIN_MTU, NetworkSpace.MAX_MTU, 1));
    }
        
    // SETTINGS CHANGE NOTIFICATION /////////
    private SettingsChangedListener settingsChangedListener;
    public void setSettingsChangedListener(SettingsChangedListener settingsChangedListener){
	this.settingsChangedListener = settingsChangedListener;
    }
    ///////////////////////////////////////////
    
    public void doSave(Object settings, boolean validateOnly) throws Exception {
        
        // NAT ENABLED ///////////
        boolean natEnabled = natEnabledJRadioButton.isSelected();

	NetworkSpace natSpace = null;
	IPaddr natAddress = null;
	if( natEnabled ){
	    // NAT ADDRESS SPACE //
	    natSpace = (NetworkSpace) natSpaceJComboBox.getSelectedItem();
	    
	    // NAT ADDRESS IP ADDRESS //
	    natAddress = (IPaddr) natAddressJComboBox.getSelectedItem();
	}

	// NETWORK ALIASES //
	List<IPNetworkRule> aliases = null;
	try{
	    aliases = (List<IPNetworkRule>) IPNetworkRule.parseList(aliasJTextArea.getText());
	}
	catch(Exception e){
	    aliasJTextArea.setBackground( Util.INVALID_BACKGROUND_COLOR );
	    throw new Exception(EXCEPTION_ALIAS);
	}

	// TRAFFIC FORWARDING //
	boolean forwardingEnabled = forwardingEnabledJRadioButton.isSelected();

	// MTU //
	int mtu = (Integer) mtuJSpinner.getValue();
        
        // SAVE THE VALUES ////////////////////////////////////
	if( !validateOnly ){
	    NetworkSpacesSettings networkSpacesSettings = (NetworkSpacesSettings) settings;
	    NetworkSpace thisNetworkSpace = null;
	    for(NetworkSpace networkSpace : networkSpacesSettings.getNetworkSpaceList() )
		if( networkSpace.getName().equals(this.networkSpace.getName()) ) // XXX this may not be kosher
		    thisNetworkSpace = networkSpace;
	    if( thisNetworkSpace == null )
		return;

	    thisNetworkSpace.setIsNatEnabled(natEnabled);
	    if( natEnabled ){
		thisNetworkSpace.setNatSpace(natSpace);
		thisNetworkSpace.setNatAddress(natAddress);
	    }
	    thisNetworkSpace.setNetworkList(aliases);
	    thisNetworkSpace.setIsTrafficForwarded(forwardingEnabled);
	    thisNetworkSpace.setMtu(mtu);
	}
        
    }
    
    boolean natEnabledCurrent;
    NetworkSpace natSpaceCurrent;
    IPaddr natAddressCurrent;
    String aliasesCurrent;
    boolean forwardingEnabledCurrent;
    int mtuCurrent;

    public void doRefresh(Object settings) {
	NetworkSpacesSettings networkSpacesSettings = (NetworkSpacesSettings) settings;        
	NetworkSpace thisNetworkSpace = null;
	for(NetworkSpace networkSpace : networkSpacesSettings.getNetworkSpaceList() )
	    if( networkSpace.getName().equals(this.networkSpace.getName()) ) // XXX this may not be kosher
		thisNetworkSpace = networkSpace;

        // ENABLED ///////////
	natEnabledCurrent = thisNetworkSpace.getIsNatEnabled();
	this.setNatEnabledDependency(natEnabledCurrent);
	if( natEnabledCurrent )
	    natEnabledJRadioButton.setSelected(true);
	else
	    natDisabledJRadioButton.setSelected(true);
        
	// NAT ADDRESS SPACE //
	natSpaceCurrent = thisNetworkSpace.getNatSpace();
	natSpaceJComboBox.setSelectedItem( natSpaceCurrent );	
	
	// NAT ADDRESS IP ADDRESS //
	natAddressCurrent = thisNetworkSpace.getNatAddress();
	natAddressJComboBox.setSelectedItem( natAddressCurrent );	

	// NETWORK ALIASES //
	aliasesCurrent = thisNetworkSpace.getNetworkList().toString();
	aliasJTextArea.setBackground( Util.INVALID_BACKGROUND_COLOR );
	aliasJTextArea.setText( aliasesCurrent );

	// TRAFFIC FORWARDING //
	forwardingEnabledCurrent = thisNetworkSpace.getIsTrafficForwarded();
	if( forwardingEnabledCurrent )
	    forwardingEnabledJRadioButton.setSelected(true);
	else
	    forwardingDisabledJRadioButton.setSelected(true);

	// MTU //
	mtuCurrent = thisNetworkSpace.getMtu();
	mtuJSpinner.setValue(mtuCurrent);
                        
    }

        
    

    
        private void initComponents() {//GEN-BEGIN:initComponents
                java.awt.GridBagConstraints gridBagConstraints;

                natButtonGroup = new javax.swing.ButtonGroup();
                forwardingButtonGroup = new javax.swing.ButtonGroup();
                natJPanel = new javax.swing.JPanel();
                jTextArea2 = new javax.swing.JTextArea();
                jPanel1 = new javax.swing.JPanel();
                natEnabledJRadioButton = new javax.swing.JRadioButton();
                natDisabledJRadioButton = new javax.swing.JRadioButton();
                jLabel1 = new javax.swing.JLabel();
                jSeparator1 = new javax.swing.JSeparator();
                jLabel2 = new javax.swing.JLabel();
                jPanel2 = new javax.swing.JPanel();
                natSpaceJLabel = new javax.swing.JLabel();
                natSpaceJComboBox = new javax.swing.JComboBox();
                natAddressJLabel = new javax.swing.JLabel();
                natAddressJComboBox = new javax.swing.JComboBox();
                aliasJPanel = new javax.swing.JPanel();
                jTextArea3 = new javax.swing.JTextArea();
                restrictIPJPanel = new javax.swing.JPanel();
                jScrollPane1 = new javax.swing.JScrollPane();
                aliasJTextArea = new javax.swing.JTextArea();
                forwardingJPanel = new javax.swing.JPanel();
                jLabel3 = new javax.swing.JLabel();
                jPanel3 = new javax.swing.JPanel();
                forwardingEnabledJRadioButton = new javax.swing.JRadioButton();
                forwardingDisabledJRadioButton = new javax.swing.JRadioButton();
                jLabel4 = new javax.swing.JLabel();
                mtuJPanel = new javax.swing.JPanel();
                jLabel5 = new javax.swing.JLabel();
                jPanel4 = new javax.swing.JPanel();
                jLabel6 = new javax.swing.JLabel();
                mtuJSpinner = new javax.swing.JSpinner();

                setLayout(new java.awt.GridBagLayout());

                setMinimumSize(new java.awt.Dimension(530, 731));
                setPreferredSize(new java.awt.Dimension(530, 731));
                natJPanel.setLayout(new java.awt.GridBagLayout());

                natJPanel.setBorder(new javax.swing.border.TitledBorder(null, "NAT (Network Address Translation)", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
                jTextArea2.setEditable(false);
                jTextArea2.setLineWrap(true);
                jTextArea2.setText("NAT allows multiple computers in the internal network to share internet access through a single shared public IP address.");
                jTextArea2.setWrapStyleWord(true);
                jTextArea2.setOpaque(false);
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 15, 0, 15);
                natJPanel.add(jTextArea2, gridBagConstraints);

                jPanel1.setLayout(new java.awt.GridBagLayout());

                natButtonGroup.add(natEnabledJRadioButton);
                natEnabledJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
                natEnabledJRadioButton.setText("Enabled");
                natEnabledJRadioButton.setFocusPainted(false);
                natEnabledJRadioButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                natEnabledJRadioButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                jPanel1.add(natEnabledJRadioButton, gridBagConstraints);

                natButtonGroup.add(natDisabledJRadioButton);
                natDisabledJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
                natDisabledJRadioButton.setText("Disabled");
                natDisabledJRadioButton.setFocusPainted(false);
                natDisabledJRadioButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                natDisabledJRadioButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 2;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                jPanel1.add(natDisabledJRadioButton, gridBagConstraints);

                jLabel1.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel1.setText("NAT ");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.gridheight = 2;
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                jPanel1.add(jLabel1, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.insets = new java.awt.Insets(15, 0, 0, 0);
                natJPanel.add(jPanel1, gridBagConstraints);

                jSeparator1.setForeground(new java.awt.Color(200, 200, 200));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 2;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                natJPanel.add(jSeparator1, gridBagConstraints);

                jLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel2.setText("<html>You must choose a NAT address, which is the address that traffic from this Space will appear to be coming from.</html>");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 3;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(10, 15, 0, 15);
                natJPanel.add(jLabel2, gridBagConstraints);

                jPanel2.setLayout(new java.awt.GridBagLayout());

                natSpaceJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                natSpaceJLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
                natSpaceJLabel.setText("Space: ");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                jPanel2.add(natSpaceJLabel, gridBagConstraints);

                natSpaceJComboBox.setMaximumSize(new java.awt.Dimension(200, 24));
                natSpaceJComboBox.setMinimumSize(new java.awt.Dimension(200, 24));
                natSpaceJComboBox.setPreferredSize(new java.awt.Dimension(200, 24));
                natSpaceJComboBox.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                natSpaceJComboBoxActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                jPanel2.add(natSpaceJComboBox, gridBagConstraints);

                natAddressJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                natAddressJLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
                natAddressJLabel.setText("IP Address: ");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
                jPanel2.add(natAddressJLabel, gridBagConstraints);

                natAddressJComboBox.setMaximumSize(new java.awt.Dimension(200, 24));
                natAddressJComboBox.setMinimumSize(new java.awt.Dimension(200, 24));
                natAddressJComboBox.setPreferredSize(new java.awt.Dimension(200, 24));
                natAddressJComboBox.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                natAddressJComboBoxActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
                jPanel2.add(natAddressJComboBox, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 4;
                gridBagConstraints.insets = new java.awt.Insets(10, 0, 5, 0);
                natJPanel.add(jPanel2, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
                add(natJPanel, gridBagConstraints);

                aliasJPanel.setLayout(new java.awt.GridBagLayout());

                aliasJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Network Aliases", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
                jTextArea3.setEditable(false);
                jTextArea3.setLineWrap(true);
                jTextArea3.setText("Network Aliases are the networks (masked IP addresses) that this Space will accept traffic from.");
                jTextArea3.setWrapStyleWord(true);
                jTextArea3.setOpaque(false);
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 15, 0, 15);
                aliasJPanel.add(jTextArea3, gridBagConstraints);

                restrictIPJPanel.setLayout(new java.awt.GridBagLayout());

                jScrollPane1.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                jScrollPane1.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
                aliasJTextArea.setWrapStyleWord(true);
                aliasJTextArea.addCaretListener(new javax.swing.event.CaretListener() {
                        public void caretUpdate(javax.swing.event.CaretEvent evt) {
                                aliasJTextAreaCaretUpdate(evt);
                        }
                });

                jScrollPane1.setViewportView(aliasJTextArea);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                restrictIPJPanel.add(jScrollPane1, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.ipady = 100;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(15, 15, 10, 15);
                aliasJPanel.add(restrictIPJPanel, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
                add(aliasJPanel, gridBagConstraints);

                forwardingJPanel.setLayout(new java.awt.GridBagLayout());

                forwardingJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Traffic Forwarding", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
                jLabel3.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel3.setText("<html>Traffic Forwarding allows traffic to flow between two different Spaces.  If this is disabled, this Space will be isolated from communicating with other Spaces.</html>");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(10, 15, 0, 15);
                forwardingJPanel.add(jLabel3, gridBagConstraints);

                jPanel3.setLayout(new java.awt.GridBagLayout());

                forwardingButtonGroup.add(forwardingEnabledJRadioButton);
                forwardingEnabledJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
                forwardingEnabledJRadioButton.setText("Enabled");
                forwardingEnabledJRadioButton.setFocusPainted(false);
                forwardingEnabledJRadioButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                forwardingEnabledJRadioButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                jPanel3.add(forwardingEnabledJRadioButton, gridBagConstraints);

                forwardingButtonGroup.add(forwardingDisabledJRadioButton);
                forwardingDisabledJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
                forwardingDisabledJRadioButton.setText("Disabled");
                forwardingDisabledJRadioButton.setFocusPainted(false);
                forwardingDisabledJRadioButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                forwardingDisabledJRadioButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 2;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                jPanel3.add(forwardingDisabledJRadioButton, gridBagConstraints);

                jLabel4.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel4.setText("Traffic Forwarding");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.gridheight = 2;
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                jPanel3.add(jLabel4, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.insets = new java.awt.Insets(15, 0, 0, 0);
                forwardingJPanel.add(jPanel3, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
                add(forwardingJPanel, gridBagConstraints);

                mtuJPanel.setLayout(new java.awt.GridBagLayout());

                mtuJPanel.setBorder(new javax.swing.border.TitledBorder(null, "MTU (Maximum Transfer Unit)", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
                jLabel5.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel5.setText("<html>The MTU specifies the maximum amount of data per packet that should be transferred out of this Space.  This value should not be changed unless explicitly necessary.</html>");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(10, 15, 0, 15);
                mtuJPanel.add(jLabel5, gridBagConstraints);

                jPanel4.setLayout(new java.awt.GridBagLayout());

                jLabel6.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel6.setText("MTU (bytes) ");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                jPanel4.add(jLabel6, gridBagConstraints);

                mtuJSpinner.setMaximumSize(new java.awt.Dimension(100, 20));
                mtuJSpinner.setMinimumSize(new java.awt.Dimension(100, 20));
                mtuJSpinner.setPreferredSize(new java.awt.Dimension(100, 20));
                mtuJSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
                        public void stateChanged(javax.swing.event.ChangeEvent evt) {
                                mtuJSpinnerStateChanged(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                jPanel4.add(mtuJSpinner, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.insets = new java.awt.Insets(15, 0, 5, 0);
                mtuJPanel.add(jPanel4, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
                add(mtuJPanel, gridBagConstraints);

        }//GEN-END:initComponents

		private void aliasJTextAreaCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_aliasJTextAreaCaretUpdate
		    if( !aliasJTextArea.getText().equals(aliasesCurrent) && (settingsChangedListener != null) )
			settingsChangedListener.settingsChanged(this);
		}//GEN-LAST:event_aliasJTextAreaCaretUpdate

		private void mtuJSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_mtuJSpinnerStateChanged
		    if( !mtuJSpinner.getValue().equals(mtuCurrent) && (settingsChangedListener != null) )
			settingsChangedListener.settingsChanged(this);
		}//GEN-LAST:event_mtuJSpinnerStateChanged

		private void natAddressJComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_natAddressJComboBoxActionPerformed
		    if( !natAddressCurrent.equals(((IPaddr)natSpaceJComboBox.getSelectedItem())) && (settingsChangedListener != null) )
			settingsChangedListener.settingsChanged(this);
		}//GEN-LAST:event_natAddressJComboBoxActionPerformed

		private void natSpaceJComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_natSpaceJComboBoxActionPerformed
		    if( !natSpaceCurrent.getName().equals(((NetworkSpace)natSpaceJComboBox.getSelectedItem()).getName()) && (settingsChangedListener != null) )
			settingsChangedListener.settingsChanged(this);
		}//GEN-LAST:event_natSpaceJComboBoxActionPerformed

		private void forwardingDisabledJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_forwardingDisabledJRadioButtonActionPerformed
		    if( forwardingEnabledCurrent && (settingsChangedListener != null) )
			settingsChangedListener.settingsChanged(this);
		}//GEN-LAST:event_forwardingDisabledJRadioButtonActionPerformed

		private void forwardingEnabledJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_forwardingEnabledJRadioButtonActionPerformed
		    if( !forwardingEnabledCurrent && (settingsChangedListener != null) )
			settingsChangedListener.settingsChanged(this);
		}//GEN-LAST:event_forwardingEnabledJRadioButtonActionPerformed

        
    private void natDisabledJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_natDisabledJRadioButtonActionPerformed
        this.setNatEnabledDependency(false);
	if( natEnabledCurrent && (settingsChangedListener != null) )
	    settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_natDisabledJRadioButtonActionPerformed

    private void natEnabledJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_natEnabledJRadioButtonActionPerformed
        this.setNatEnabledDependency(true);
	if( !natEnabledCurrent && (settingsChangedListener != null) )
	    settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_natEnabledJRadioButtonActionPerformed
    
    private void setNatEnabledDependency(boolean enabled){
	natSpaceJLabel.setEnabled( enabled );
	natSpaceJComboBox.setEnabled( enabled );
	natAddressJLabel.setEnabled( enabled );
	natAddressJComboBox.setEnabled( enabled );
    }
    
        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JPanel aliasJPanel;
        private javax.swing.JTextArea aliasJTextArea;
        private javax.swing.ButtonGroup forwardingButtonGroup;
        public javax.swing.JRadioButton forwardingDisabledJRadioButton;
        public javax.swing.JRadioButton forwardingEnabledJRadioButton;
        private javax.swing.JPanel forwardingJPanel;
        private javax.swing.JLabel jLabel1;
        private javax.swing.JLabel jLabel2;
        private javax.swing.JLabel jLabel3;
        private javax.swing.JLabel jLabel4;
        private javax.swing.JLabel jLabel5;
        private javax.swing.JLabel jLabel6;
        private javax.swing.JPanel jPanel1;
        private javax.swing.JPanel jPanel2;
        private javax.swing.JPanel jPanel3;
        private javax.swing.JPanel jPanel4;
        private javax.swing.JScrollPane jScrollPane1;
        private javax.swing.JSeparator jSeparator1;
        private javax.swing.JTextArea jTextArea2;
        private javax.swing.JTextArea jTextArea3;
        private javax.swing.JPanel mtuJPanel;
        private javax.swing.JSpinner mtuJSpinner;
        private javax.swing.JComboBox natAddressJComboBox;
        private javax.swing.JLabel natAddressJLabel;
        private javax.swing.ButtonGroup natButtonGroup;
        public javax.swing.JRadioButton natDisabledJRadioButton;
        public javax.swing.JRadioButton natEnabledJRadioButton;
        private javax.swing.JPanel natJPanel;
        private javax.swing.JComboBox natSpaceJComboBox;
        private javax.swing.JLabel natSpaceJLabel;
        private javax.swing.JPanel restrictIPJPanel;
        // End of variables declaration//GEN-END:variables
    
}
