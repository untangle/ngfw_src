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
import java.awt.Component;
import javax.swing.JTabbedPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.table.*;
import javax.swing.event.*;
import javax.swing.border.*;

import com.metavize.tran.nat.*;
import com.metavize.mvvm.networking.*;

public class MTransformControlsJPanel extends com.metavize.gui.transform.MTransformControlsJPanel{

    private static final String NAME_NET_SPACES       = "Net Spaces";
    private static final String NAME_INTERFACE_MAP    = "Interface-To-Space Map";
    private static final String NAME_SPACE            = "Space";
    private static final String NAME_ROUTING          = "Routing";
    private static final String NAME_NAT              = "NAT";
    private static final String NAME_DMZ              = "DMZ";
    private static final String NAME_REDIRECT         = "Redirect";
    private static final String NAME_DHCP             = "DHCP";
    private static final String NAME_DHCP_SETTINGS    = "Settings";
    private static final String NAME_DHCP_ADDRESS_MAP = "Address Map";
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

    private boolean baseGuiBuilt = false;

    private static Nat natTransform;
    public static Nat getNatTransform(){ return natTransform; }
    private SetupState setupState, lastSetupState;
    private List<NetworkSpace> networkSpaceList;    


    private Component natScrollable;    
    private Component dmzScrollable;
    private JTabbedPane spacesJTabbedPane;
    private InterfaceMapJPanel interfaceMapJPanel;
    private List<String> spaceNameList;
    private List<Component> spaceScrollableList;
    private RoutingJPanel routingJPanel;

    private void generateSpecificGui(){
	if(SetupState.BASIC.equals(setupState)){
	    // REMOVE ADVANCED STUFF IF IT WAS THERE //
	    if(spacesJTabbedPane != null){
		removeTab(spacesJTabbedPane);
		spacesJTabbedPane = null;
	    }
	    if(interfaceMapJPanel != null){
		removeTab(interfaceMapJPanel);
		removeSavable(NAME_INTERFACE_MAP);
		removeRefreshable(NAME_INTERFACE_MAP);
		interfaceMapJPanel = null;
	    }
	    if(spaceNameList != null){
		for( String name : spaceNameList ){
		    removeSavable(name);
		    removeRefreshable(name);
		}
		spaceNameList = null;
	    }
	    if(routingJPanel != null){
		removeTab(routingJPanel);
		removeSavable(NAME_ROUTING);
		removeRefreshable(NAME_ROUTING);
		routingJPanel = null;
	    }

	    // NAT ///////////////
	    if( natScrollable == null ){
		NatJPanel natJPanel = new NatJPanel();
		natScrollable = addScrollableTab(0, null, NAME_NAT, null, natJPanel, false, true);
		addSavable(NAME_NAT, natJPanel);
		addRefreshable(NAME_NAT, natJPanel);
		natJPanel.setSettingsChangedListener(this);
	    }
	    
	    // DMZ ////////////////
	    if( dmzScrollable == null ){
		DmzJPanel dmzJPanel = new DmzJPanel();
		dmzScrollable = addScrollableTab(1, null, NAME_DMZ, null, dmzJPanel, false, true);
		addSavable(NAME_DMZ, dmzJPanel);
		addRefreshable(NAME_DMZ, dmzJPanel);
		dmzJPanel.setSettingsChangedListener(this);
	    }
	}
	else if(SetupState.ADVANCED.equals(setupState)){
	    // REMOVE BASIC STUFF IF IT WAS THERE //
	    if(natScrollable != null){
		removeTab(natScrollable);
		removeSavable(NAME_NAT);
		removeRefreshable(NAME_NAT);
		natScrollable = null;
	    }
	    if(dmzScrollable != null){
		removeTab(dmzScrollable);
		removeSavable(NAME_DMZ);
		removeRefreshable(NAME_DMZ);
		dmzScrollable = null;
	    }

	    // NET SPACES
	    if( spacesJTabbedPane == null ){
		spacesJTabbedPane = addTabbedPane(0, NAME_NET_SPACES, null);
	    }

	    // INTERFACE MAP //
	    if( interfaceMapJPanel == null ){
		interfaceMapJPanel = new InterfaceMapJPanel();
		spacesJTabbedPane.addTab(NAME_INTERFACE_MAP, null, interfaceMapJPanel);
		addSavable(NAME_INTERFACE_MAP, interfaceMapJPanel);
		addRefreshable(NAME_INTERFACE_MAP, interfaceMapJPanel);
		interfaceMapJPanel.setSettingsChangedListener(this);
	    }

	    // SPACES //
	    if( spaceNameList != null ){
		for( String name : spaceNameList ){
		    removeSavable(name);
		    removeRefreshable(name);
		}		
		for( Component component : spaceScrollableList ){
		    spacesJTabbedPane.remove(component);
		}		
	    }
	    spaceNameList = new ArrayList<String>();
	    spaceScrollableList = new ArrayList<Component>();
	    int index = 0;
	    for( NetworkSpace networkSpace : networkSpaceList ){
		SpaceJPanel spaceJPanel = new SpaceJPanel(networkSpace);
		spaceNameList.add(Integer.toString(index) + NAME_SPACE + networkSpace.getName());
		spaceScrollableList.add( addScrollableTab(spacesJTabbedPane, NAME_SPACE 
							  + " (" + networkSpace.getName() + ")", 
							  null, spaceJPanel, false, true) );
		addSavable( Integer.toString(index) + NAME_SPACE + networkSpace.getName(), spaceJPanel);
		addRefreshable( Integer.toString(index) + NAME_SPACE + networkSpace.getName(), spaceJPanel);
		spaceJPanel.setSettingsChangedListener(this);
		index++;
	    }

	    // ROUTING //
	    if( routingJPanel == null ){
		routingJPanel = new RoutingJPanel();
		addTab(1, NAME_ROUTING, null, routingJPanel);
		addSavable(NAME_ROUTING, routingJPanel);
		addRefreshable(NAME_ROUTING, routingJPanel);
		routingJPanel.setSettingsChangedListener(this);
	    }
	}
	else{
	    // SOME BAD SHITE HAPPENED
	}

    }

    public void generateGui(){

	// GENERATE SPECIFIC GUI //
	generateSpecificGui();

        // REDIRECT /////////////
        RedirectJPanel redirectJPanel = new RedirectJPanel();
        addTab(NAME_REDIRECT, null, redirectJPanel);
        addSavable(NAME_REDIRECT, redirectJPanel);
	addRefreshable(NAME_REDIRECT, redirectJPanel);
	redirectJPanel.setSettingsChangedListener(this);

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
        dnsJTabbedPane.addTab(NAME_DNS + " " + NAME_DNS_ADDRESS_MAP, null, dnsAddressJPanel);
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

	baseGuiBuilt = true;
    }

    public void refreshAll() throws Exception {
	super.refreshAll();
	setupState = ((NatCommonSettings)settings).getSetupState();
	if( lastSetupState == null )
	    lastSetupState = setupState;
	natTransform = (Nat) mTransformJPanel.getTransformContext().transform();
	if(SetupState.ADVANCED.equals( setupState )){
	    networkSpaceList = ((NetworkSpacesSettings)settings).getNetworkSpaceList();
	}
	if( baseGuiBuilt ){
	    SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
		generateSpecificGui();
	    }});
	}
	if( !setupState.equals(lastSetupState) ){
	    SwingUtilities.invokeLater( new Runnable(){ public void run(){
		MTransformControlsJPanel.this.getMTabbedPane().setSelectedIndex(0);
	    }});
	}
	lastSetupState = setupState;
    }
        
}
