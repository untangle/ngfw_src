/*
 * $HeadURL:$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.router.gui;

import java.awt.Color;
import java.util.List;
import javax.swing.*;

import com.untangle.gui.node.*;
import com.untangle.gui.util.Util;
import com.untangle.uvm.networking.*;
import com.untangle.uvm.node.*;
import com.untangle.node.router.*;


public class SpaceJPanel extends javax.swing.JPanel implements Savable<Object>, Refreshable<Object> {

    private static final String EXCEPTION_ALIAS = "The Network Aliases must be a comma separated list of masked IP Addresses.";
    private static final String EXCEPTION_NATTED_ADDRESS = "You must choose the NATted address for this space.";
    private static final String EXCEPTION_MTU = "The MTU must be an integer value between "
        + NetworkSpace.MIN_MTU + " and " + NetworkSpace.MAX_MTU + ".";

    private NetworkSpace initNetworkSpace;

    public SpaceJPanel(NetworkSpace networkSpace) {
        initNetworkSpace = networkSpace;
        initComponents();
        mtuJSpinner.setModel(new SpinnerNumberModel(NetworkSpace.DEFAULT_MTU, NetworkSpace.MIN_MTU, NetworkSpace.MAX_MTU, 1));
        ((JSpinner.NumberEditor)mtuJSpinner.getEditor()).getFormat().setGroupingUsed(false);
        aliasJScrollPane.getVerticalScrollBar().setFocusable(false);
        Util.addPanelFocus(this, natEnabledJRadioButton);
        Util.addFocusHighlight(aliasJTextArea);
        Util.addFocusHighlight(mtuJSpinner);
    }

    // SETTINGS CHANGE NOTIFICATION /////////
    private SettingsChangedListener settingsChangedListener;
    public void setSettingsChangedListener(SettingsChangedListener settingsChangedListener){
        this.settingsChangedListener = settingsChangedListener;
    }
    ///////////////////////////////////////////

    public void doSave(Object settings, boolean validateOnly) throws Exception {

        // DETERMINE IF THE SPACE WAS REMOVED
        NetworkSpacesSettings networkSpacesSettings = (NetworkSpacesSettings) settings;
        NetworkSpace thisNetworkSpace = null;
        List<NetworkSpace> networkSpaceList = (List<NetworkSpace>) networkSpacesSettings.getNetworkSpaceList();
        for(NetworkSpace networkSpace : networkSpaceList )
            if( networkSpace.getBusinessPapers() == initNetworkSpace.getBusinessPapers() )
                thisNetworkSpace = networkSpace;
        if( thisNetworkSpace == null )
            return;

        // NAT ENABLED ///////////
        boolean natEnabled = natEnabledJRadioButton.isSelected();

        NetworkSpace natSpace = null;
        IPaddr natAddress = null;
        if( natEnabled ){
            // NAT ADDRESS SPACE //
            natSpace = ((NattedPair) nattedJComboBox.getSelectedItem()).getNetworkSpace();

            // NAT ADDRESS IP ADDRESS //
            natAddress = ((NattedPair) nattedJComboBox.getSelectedItem()).getNetworkAddress();

            if( (natSpace==null) && (natAddress==null) ){
                nattedJComboBox.setBackground( Util.INVALID_BACKGROUND_COLOR );
                throw new Exception(EXCEPTION_NATTED_ADDRESS);
            }
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
        ((JSpinner.DefaultEditor)mtuJSpinner.getEditor()).getTextField().setBackground(Color.WHITE);
        int mtu = 0;
        try{ mtuJSpinner.commitEdit(); }
        catch(Exception e){
            ((JSpinner.DefaultEditor)mtuJSpinner.getEditor()).getTextField().setBackground(Util.INVALID_BACKGROUND_COLOR);
            throw new Exception(EXCEPTION_MTU);
        }
        mtu = (Integer) mtuJSpinner.getValue();

        // SAVE THE VALUES ////////////////////////////////////
        if( !validateOnly ){
            thisNetworkSpace.setIsNatEnabled(natEnabled);
            if( natEnabled ){
                thisNetworkSpace.setNatSpace(natSpace);
                thisNetworkSpace.setNatAddress(natAddress);
            }
            thisNetworkSpace.setNetworkList(aliases);
            thisNetworkSpace.setIsTrafficForwarded(forwardingEnabled);
            thisNetworkSpace.setMtu(mtu);
            networkSpacesSettings.setNetworkSpaceList(networkSpaceList);
        }

    }

    String nameCurrent;
    boolean natEnabledCurrent;
    NattedPair nattedPairCurrent;
    String aliasesCurrent;
    boolean forwardingEnabledCurrent;
    int mtuCurrent;

    private class NattedPair {
        private NetworkSpace networkSpace;
        private IPaddr networkAddress;
        public NattedPair(NetworkSpace networkSpace, IPaddr networkAddress){
            this.networkSpace = networkSpace;
            this.networkAddress = networkAddress;
        }
        public String toString(){
            if( (networkSpace==null) && (networkAddress==null) )
                return "Please make a selection...";
            else
                return "Space: " + networkSpace.getName() + (networkAddress==null?"":"    Address: " + networkAddress.toString());
        }
        public NetworkSpace getNetworkSpace(){ return networkSpace; }
        public IPaddr getNetworkAddress(){ return networkAddress; }
        public boolean equals(Object o){
            if( !(o instanceof NattedPair) || (o==null))
                return false;
            NattedPair other = (NattedPair) o;
            return isEqual(networkSpace,other.getNetworkSpace())
                && Util.isEqual(networkAddress,other.getNetworkAddress());
        }

        public boolean isEqual( NetworkSpace a, NetworkSpace b )
        {
            if (( a == null ) != ( b == null )) return false;
            if ( a == null ) return true;
            return ( a.getBusinessPapers() == b.getBusinessPapers());
        }

        public boolean isEmpty(){
            return (networkSpace==null) && (networkAddress==null);
        }
    }

    public void doRefresh(Object settings) {
        NetworkSpacesSettings networkSpacesSettings = (NetworkSpacesSettings) settings;
        NetworkSpace thisNetworkSpace = null;
        for(NetworkSpace networkSpace : networkSpacesSettings.getNetworkSpaceList() )
            if( networkSpace.getBusinessPapers() == initNetworkSpace.getBusinessPapers() )
                thisNetworkSpace = networkSpace;

        // POPULATE NAT MAP //
        nattedJComboBox.setEnabled(false);
        nattedJComboBox.removeAllItems();
        for(NetworkSpace networkSpace : networkSpacesSettings.getNetworkSpaceList() ) {
            if( networkSpace.getBusinessPapers() != initNetworkSpace.getBusinessPapers() ){

                boolean isFirst = true;

                /* If DHCP is enabled, add an address with no address */
                if (networkSpace.getIsDhcpEnabled()) {
                    NattedPair nattedPair = new NattedPair(networkSpace,null);
                    nattedJComboBox.addItem( nattedPair );
                    isFirst = false;
                }

                for( IPNetworkRule address : (List<IPNetworkRule>) networkSpace.getNetworkList() ){
                    NattedPair nattedPair;
                    if (isFirst) nattedPair = new NattedPair(networkSpace,null);
                    else         nattedPair = new NattedPair(networkSpace,address.getNetwork());
                    nattedJComboBox.addItem( nattedPair );
                    isFirst = false;
                }
            }
        }
        nattedPairCurrent = new NattedPair(thisNetworkSpace.getNatSpace(),thisNetworkSpace.getNatAddress());
        nattedJComboBox.setSelectedItem(nattedPairCurrent);
        nattedJComboBox.setBackground( Util.VALID_BACKGROUND_COLOR );
        nattedJComboBox.setEnabled(true);

        // ENABLED ///////////
        natEnabledCurrent = thisNetworkSpace.getIsNatEnabled();
        this.setRouterEnabledDependency(natEnabledCurrent);
        if( natEnabledCurrent )
            natEnabledJRadioButton.setSelected(true);
        else
            natDisabledJRadioButton.setSelected(true);

        // NETWORK ALIASES //
        aliasesCurrent = "";
        List<IPNetworkRule> aliases = (List<IPNetworkRule>) thisNetworkSpace.getNetworkList();
        int aliasIndex = 0;
        for( IPNetworkRule alias : aliases ){
            aliasesCurrent += alias.toString();
            if( aliasIndex < aliases.size()-1)
                aliasesCurrent += ", ";
            aliasIndex++;
        }
        aliasJTextArea.setBackground( Color.WHITE );
        aliasJTextArea.setText( aliasesCurrent );

        // TRAFFIC FORWARDING //
        forwardingEnabledCurrent = thisNetworkSpace.getIsTrafficForwarded();
        if( forwardingEnabledCurrent )
            forwardingEnabledJRadioButton.setSelected(true);
        else
            forwardingDisabledJRadioButton.setSelected(true);

        // MTU //
        mtuCurrent = thisNetworkSpace.getMtu();
        mtuJSpinner.setValue( mtuCurrent );
        ((JSpinner.DefaultEditor)mtuJSpinner.getEditor()).getTextField().setText(Integer.toString(mtuCurrent));
        ((JSpinner.DefaultEditor)mtuJSpinner.getEditor()).getTextField().setBackground(Color.WHITE);

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
        nattedJLabel = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        nattedJComboBox = new javax.swing.JComboBox();
        aliasJPanel = new javax.swing.JPanel();
        jTextArea3 = new javax.swing.JTextArea();
        restrictIPJPanel = new javax.swing.JPanel();
        aliasJScrollPane = new javax.swing.JScrollPane();
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

        setMinimumSize(new java.awt.Dimension(515, 700));
        setPreferredSize(new java.awt.Dimension(515, 700));
        natJPanel.setLayout(new java.awt.GridBagLayout());

        natJPanel.setBorder(new javax.swing.border.TitledBorder(null, "NAT (Network Address Translation)", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
        jTextArea2.setEditable(false);
        jTextArea2.setLineWrap(true);
        jTextArea2.setText("NAT allows multiple computers in the internal network to share internet access through a single shared public IP address.");
        jTextArea2.setWrapStyleWord(true);
        jTextArea2.setFocusable(false);
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

        nattedJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        nattedJLabel.setText("<html>You must choose a NAT address, which is the address that traffic from this Space will appear to be coming from.</html>");
        nattedJLabel.setFocusable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 15, 0, 15);
        natJPanel.add(nattedJLabel, gridBagConstraints);

        jPanel2.setLayout(new java.awt.GridBagLayout());

        nattedJComboBox.setFont(new java.awt.Font("Dialog", 0, 12));
        nattedJComboBox.setMaximumSize(new java.awt.Dimension(400, 24));
        nattedJComboBox.setMinimumSize(new java.awt.Dimension(400, 24));
        nattedJComboBox.setPreferredSize(new java.awt.Dimension(400, 24));
        nattedJComboBox.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    nattedJComboBoxActionPerformed(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        jPanel2.add(nattedJComboBox, gridBagConstraints);

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
        jTextArea3.setFocusable(false);
        jTextArea3.setOpaque(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 15, 0, 15);
        aliasJPanel.add(jTextArea3, gridBagConstraints);

        restrictIPJPanel.setLayout(new java.awt.GridBagLayout());

        aliasJScrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        aliasJScrollPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        aliasJTextArea.setLineWrap(true);
        aliasJTextArea.setWrapStyleWord(true);
        aliasJTextArea.addCaretListener(new javax.swing.event.CaretListener() {
                public void caretUpdate(javax.swing.event.CaretEvent evt) {
                    aliasJTextAreaCaretUpdate(evt);
                }
            });

        aliasJScrollPane.setViewportView(aliasJTextArea);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        restrictIPJPanel.add(aliasJScrollPane, gridBagConstraints);

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
        jLabel3.setFocusable(false);
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
        jLabel5.setFocusable(false);
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

        mtuJSpinner.setFont(new java.awt.Font("Dialog", 0, 12));
        mtuJSpinner.setMaximumSize(new java.awt.Dimension(75, 19));
        mtuJSpinner.setMinimumSize(new java.awt.Dimension(75, 19));
        mtuJSpinner.setPreferredSize(new java.awt.Dimension(75, 19));
        mtuJSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
                public void stateChanged(javax.swing.event.ChangeEvent evt) {
                    mtuJSpinnerStateChanged(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
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

    private void mtuJSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_mtuJSpinnerStateChanged
        if( !mtuJSpinner.getValue().equals(mtuCurrent) && (settingsChangedListener != null) )
            settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_mtuJSpinnerStateChanged

    private void aliasJTextAreaCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_aliasJTextAreaCaretUpdate
        if( !aliasJTextArea.getText().equals(aliasesCurrent) && (settingsChangedListener != null) )
            settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_aliasJTextAreaCaretUpdate

    private void nattedJComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nattedJComboBoxActionPerformed
        if( (nattedPairCurrent!=null) && (nattedJComboBox.getSelectedItem()!=null) )
            if( !nattedPairCurrent.equals(((NattedPair)nattedJComboBox.getSelectedItem())) && (settingsChangedListener != null) )
                settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_nattedJComboBoxActionPerformed

    private void forwardingDisabledJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_forwardingDisabledJRadioButtonActionPerformed
        if( forwardingEnabledCurrent && (settingsChangedListener != null) )
            settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_forwardingDisabledJRadioButtonActionPerformed

    private void forwardingEnabledJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_forwardingEnabledJRadioButtonActionPerformed
        if( !forwardingEnabledCurrent && (settingsChangedListener != null) )
            settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_forwardingEnabledJRadioButtonActionPerformed


    private void natDisabledJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_natDisabledJRadioButtonActionPerformed
        this.setRouterEnabledDependency(false);
        if( natEnabledCurrent && (settingsChangedListener != null) )
            settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_natDisabledJRadioButtonActionPerformed

    private void natEnabledJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_natEnabledJRadioButtonActionPerformed
        this.setRouterEnabledDependency(true);
        if( !natEnabledCurrent && (settingsChangedListener != null) )
            settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_natEnabledJRadioButtonActionPerformed

    private void setRouterEnabledDependency(boolean enabled){
        nattedJLabel.setEnabled( enabled );
        nattedJComboBox.setEnabled( enabled );
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel aliasJPanel;
    private javax.swing.JScrollPane aliasJScrollPane;
    private javax.swing.JTextArea aliasJTextArea;
    private javax.swing.ButtonGroup forwardingButtonGroup;
    public javax.swing.JRadioButton forwardingDisabledJRadioButton;
    public javax.swing.JRadioButton forwardingEnabledJRadioButton;
    private javax.swing.JPanel forwardingJPanel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTextArea jTextArea2;
    private javax.swing.JTextArea jTextArea3;
    private javax.swing.JPanel mtuJPanel;
    private javax.swing.JSpinner mtuJSpinner;
    private javax.swing.ButtonGroup natButtonGroup;
    public javax.swing.JRadioButton natDisabledJRadioButton;
    public javax.swing.JRadioButton natEnabledJRadioButton;
    private javax.swing.JPanel natJPanel;
    private javax.swing.JComboBox nattedJComboBox;
    private javax.swing.JLabel nattedJLabel;
    private javax.swing.JPanel restrictIPJPanel;
    // End of variables declaration//GEN-END:variables

}
