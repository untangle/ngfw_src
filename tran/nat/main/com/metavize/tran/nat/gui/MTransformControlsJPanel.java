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
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.table.*;
import javax.swing.event.*;
import javax.swing.border.*;

import com.metavize.tran.nat.*;
import com.metavize.mvvm.networking.*;
import com.metavize.mvvm.client.MvvmRemoteContextFactory;

public class MTransformControlsJPanel extends com.metavize.gui.transform.MTransformControlsJPanel{

    private static final String NAME_NET_SPACES       = "Net Spaces";
    private static final String NAME_INTERFACE_MAP    = "Interface-To-Space Map";
    private static final String NAME_SPACE_LIST       = "Space List";
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
    private SetupState setupState;
    private List<NetworkSpace> networkSpaceList;    


    private Component natScrollable;    
    private Component dmzScrollable;
    private JTabbedPane spacesJTabbedPane;
    private InterfaceMapJPanel interfaceMapJPanel;
    private SpaceListJPanel spaceListJPanel;
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
	    if(spaceListJPanel != null){
		removeTab(spaceListJPanel);
		removeSavable(NAME_SPACE_LIST);
		removeRefreshable(NAME_SPACE_LIST);
		spaceListJPanel = null;
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
		interfaceMapJPanel.setSettingsChangedListener(this);
		addSavable(NAME_INTERFACE_MAP, interfaceMapJPanel);
		addRefreshable(NAME_INTERFACE_MAP, interfaceMapJPanel);
	    }

	    // SPACE LIST //
	    if( spaceListJPanel == null ){
		spaceListJPanel = new SpaceListJPanel();
		spacesJTabbedPane.addTab(NAME_SPACE_LIST, null, spaceListJPanel);
		spaceListJPanel.setSettingsChangedListener(this);
		addSavable(NAME_SPACE_LIST, spaceListJPanel);
		addRefreshable(NAME_SPACE_LIST, spaceListJPanel);
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
	    JComponent spaceJComponent;
	    for( NetworkSpace networkSpace : networkSpaceList ){
		if( networkSpace.getIsPrimary() ){
		    PrimarySpaceJPanel primarySpaceJPanel = new PrimarySpaceJPanel(networkSpace);
		    primarySpaceJPanel.setSettingsChangedListener(this);
		    spaceJComponent = primarySpaceJPanel;
		}
		else{
		    SpaceJPanel spaceJPanel = new SpaceJPanel(networkSpace);
		    spaceJPanel.setSettingsChangedListener(this);
		    spaceJComponent = spaceJPanel;
		}
		spaceNameList.add(networkSpace.getName() + " (" + NAME_SPACE + ")");
		spaceScrollableList.add( addScrollableTab(spacesJTabbedPane, networkSpace.getName()
							  + " (" + NAME_SPACE + ")", 
							  null, spaceJComponent, false, true) );
		addSavable( networkSpace.getName() + " (" + NAME_SPACE + ")", (Savable)spaceJComponent);
		addRefreshable( networkSpace.getName() + " (" + NAME_SPACE + ")", (Refreshable)spaceJComponent);
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
        LogJPanel logJPanel = new LogJPanel(mTransformJPanel.getTransform(), this);
        addTab(NAME_LOG, null, logJPanel);
	addShutdownable(NAME_LOG, logJPanel);

	baseGuiBuilt = true;
    }

    public void saveAll() throws Exception {
	if( !(new SaveProceedDialog( mTransformJPanel.getTransformDesc().getDisplayName() )).isProceeding() )
	    return;
	int previousTimeout = MvvmRemoteContextFactory.factory().getTimeout();
	MvvmRemoteContextFactory.factory().setTimeout(Util.RECONFIGURE_NETWORK_TIMEOUT_SECONDS);		
	super.saveAll();
	MvvmRemoteContextFactory.factory().setTimeout(previousTimeout);		
    }

    public void refreshAll() throws Exception {
	super.refreshAll();
	setupState = ((NatCommonSettings)settings).getSetupState();
	natTransform = (Nat) mTransformJPanel.getTransform();
	if(SetupState.ADVANCED.equals( setupState )){
	    networkSpaceList = ((NetworkSpacesSettings)settings).getNetworkSpaceList();
	}
	if( baseGuiBuilt ){
	    SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
		int selectedIndex = MTransformControlsJPanel.this.getMTabbedPane().getSelectedIndex();
		String selectedTitle = MTransformControlsJPanel.this.getMTabbedPane().getTitleAt(selectedIndex);
		Component selectedComponent = MTransformControlsJPanel.this.getMTabbedPane().getSelectedComponent();
		JTabbedPane selectedJTabbedPane = null;
		int subSelectedIndex = -1;
		String subSelectedTitle = null;
		if( selectedComponent instanceof JTabbedPane ){
		    selectedJTabbedPane = (JTabbedPane) selectedComponent;
		    subSelectedIndex = selectedJTabbedPane.getSelectedIndex();
		    subSelectedTitle = selectedJTabbedPane.getTitleAt(subSelectedIndex);
		}
		generateSpecificGui();
		int newIndex = MTransformControlsJPanel.this.getMTabbedPane().indexOfTab(selectedTitle);
		MTransformControlsJPanel.this.getMTabbedPane().setSelectedIndex(newIndex);
		if( subSelectedIndex != -1 ){
		    int newSubIndex = selectedJTabbedPane.indexOfTab(subSelectedTitle);
		    if( newSubIndex >= 0 )
			selectedJTabbedPane.setSelectedIndex(newSubIndex);
		    else
			selectedJTabbedPane.setSelectedIndex(subSelectedIndex);
		}		
	    }});
	}
    }
        
}
