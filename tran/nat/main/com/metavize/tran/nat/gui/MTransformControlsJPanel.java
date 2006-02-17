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

import com.metavize.gui.util.*;
import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.widgets.dialogs.*;

import java.util.*;
import java.awt.Dimension;
import javax.swing.JTabbedPane;
import javax.swing.JPanel;
import javax.swing.table.*;
import javax.swing.event.*;
import javax.swing.border.*;

import com.metavize.tran.nat.*;
import com.metavize.mvvm.networking.*;

public class MTransformControlsJPanel extends com.metavize.gui.transform.MTransformControlsJPanel{

    private static final String NAME_NET_SPACES       = "Net Spaces";
    private static final String NAME_INTERFACE_MAP    = "Interface Map";
    private static final String NAME_ROUTING          = "Routing";
    private static final String NAME_NAT              = "NAT";
    private static final String NAME_DMZ              = "DMZ";
    private static final String NAME_DHCP             = "DHCP";
    private static final String NAME_DHCP_SETTINGS    = "Settings";
    private static final String NAME_DHCP_ADDRESS_MAP = "Address Map";
    private static final String NAME_REDIRECT         = "Redirect";
    private static final String NAME_DNS              = "DNS";
    private static final String NAME_DNS_FORWARDING   = "Settings";
    private static final String NAME_DNS_ADDRESS_MAP  = "Address Map";
    private static final String NAME_ADVANCED         = "Advanced";
    private static final String NAME_LOG              = "Event Log";

    protected Dimension MIN_SIZE = new Dimension(640, 480);
    protected Dimension MAX_SIZE = new Dimension(640, 1200);
        
    public MTransformControlsJPanel(MTransformJPanel mTransformJPanel) {
        super(mTransformJPanel);
    }

    private static Nat natTransform;
    public static Nat getNatTransform(){ return natTransform; }
    private SetupState setupState;
    private List<NetworkSpace> networkSpaceList;    

    public void generateGui(){
	// BASE STATE
	removeAllTabs();
	super.saveJButton.setVisible(true);
	super.reloadJButton.setVisible(true);

	if(SetupState.BASIC == setupState){
	    // NAT ///////////////
	    NatJPanel natJPanel = new NatJPanel();
	    addScrollableTab(null, NAME_NAT, null, natJPanel, false, true);
	    addSavable(NAME_NAT, natJPanel);
	    addRefreshable(NAME_NAT, natJPanel);
	    natJPanel.setSettingsChangedListener(this);
	    
	    // DMZ ////////////////
	    DmzJPanel dmzJPanel = new DmzJPanel();
	    addScrollableTab(null, NAME_DMZ, null, dmzJPanel, false, true);
	    addSavable(NAME_DMZ, dmzJPanel);
	    addRefreshable(NAME_DMZ, dmzJPanel);
	    dmzJPanel.setSettingsChangedListener(this);
	}
	else if(SetupState.ADVANCED == setupState){
	    // NET SPACES
	    JTabbedPane spacesJTabbedPane = addTabbedPane(NAME_NET_SPACES, null);

	    // INTERFACE MAP //
	    InterfaceMapJPanel interfaceMapJPanel = new InterfaceMapJPanel();
	    spacesJTabbedPane.addTab(NAME_INTERFACE_MAP, null, interfaceMapJPanel);
	    addSavable(NAME_INTERFACE_MAP, interfaceMapJPanel);
	    addRefreshable(NAME_INTERFACE_MAP, interfaceMapJPanel);
	    
	    // SPACES //
	    for( NetworkSpace networkSpace : networkSpaceList ){
		JPanel spaceJPanel = new JPanel();
		spacesJTabbedPane.addTab(networkSpace.toString(), null, spaceJPanel);
	    }

	    // ROUTING //
	    RoutingJPanel routingJPanel = new RoutingJPanel();
	    addTab(NAME_ROUTING, null, routingJPanel);
	    addSavable(NAME_ROUTING, routingJPanel);
	    addRefreshable(NAME_ROUTING, routingJPanel);
	}
	else{
	    // SOME BAD SHITE HAPPENED
	}

	// DHCP /////////////
        JTabbedPane dhcpJTabbedPane = addTabbedPane(NAME_DHCP, null);

	// DHCP SETTINGS /////
        DhcpJPanel dhcpJPanel = new DhcpJPanel();
	addScrollableTab(dhcpJTabbedPane, NAME_DHCP + " " + NAME_DHCP_SETTINGS, null, dhcpJPanel, false, true);
        addSavable(NAME_DHCP + " " + NAME_DHCP_SETTINGS, dhcpJPanel);
        addRefreshable(NAME_DHCP + " " + NAME_DHCP_SETTINGS, dhcpJPanel);
	dhcpJPanel.setSettingsChangedListener(this);

	// DHCP ADDRESSES /////
	AddressJPanel addressJPanel = new AddressJPanel();
        dhcpJTabbedPane.addTab(NAME_DHCP + " " + NAME_DHCP_ADDRESS_MAP, null, addressJPanel );
        addSavable(NAME_DHCP + " " + NAME_DHCP_ADDRESS_MAP, addressJPanel);
        addRefreshable(NAME_DHCP + " " + NAME_DHCP_ADDRESS_MAP, addressJPanel);
	addressJPanel.setSettingsChangedListener(this);

        // REDIRECT /////////////
        RedirectJPanel redirectJPanel = new RedirectJPanel();
        addTab(NAME_REDIRECT, null, redirectJPanel);
        addSavable(NAME_REDIRECT, redirectJPanel);
	addRefreshable(NAME_REDIRECT, redirectJPanel);
	redirectJPanel.setSettingsChangedListener(this);

        // DNS /////////////
        JTabbedPane dnsJTabbedPane = addTabbedPane(NAME_DNS, null);

        // DNS SETTINGS /////////////
        DnsJPanel dnsJPanel = new DnsJPanel();
	addScrollableTab(dnsJTabbedPane, NAME_DNS + " " + NAME_DNS_FORWARDING, null, dnsJPanel, false, true);
	addSavable(NAME_DNS + " " + NAME_DNS_FORWARDING, dnsJPanel);
	addRefreshable(NAME_DNS + " " + NAME_DNS_FORWARDING, dnsJPanel);
	dnsJPanel.setSettingsChangedListener(this);

	// DNS HOSTS /////
	DnsAddressJPanel dnsAddressJPanel = new DnsAddressJPanel();
        dnsJTabbedPane.addTab(NAME_DNS_ADDRESS_MAP, null, dnsAddressJPanel);
        addSavable(NAME_DNS + " " + NAME_DNS_ADDRESS_MAP, dnsAddressJPanel);
        addRefreshable(NAME_DNS + " " + NAME_DNS_ADDRESS_MAP, dnsAddressJPanel);
	dnsAddressJPanel.setSettingsChangedListener(this);

	// ADVANCED //
	AdvancedJPanel advancedJPanel = new AdvancedJPanel(this);
	addTab(NAME_ADVANCED, null, advancedJPanel);
	addRefreshable(NAME_ADVANCED, advancedJPanel);

	// EVENT LOG //
        LogJPanel logJPanel = new LogJPanel(mTransformJPanel.getTransformContext().transform(), this);
        addTab(NAME_LOG, null, logJPanel);
	addShutdownable(NAME_LOG, logJPanel);
    }

    public void refreshAll() throws Exception {
	super.refreshAll();
	setupState = ((NatCommonSettings)settings).getSetupState();
	natTransform = (Nat) mTransformJPanel.getTransformContext().transform();
	if(SetupState.ADVANCED == setupState){
	    networkSpaceList = ((NetworkSpacesSettings)settings).getNetworkSpaceList();
	}
    }
        
}
