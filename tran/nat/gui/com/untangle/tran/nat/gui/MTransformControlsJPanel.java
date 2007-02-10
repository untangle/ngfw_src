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


package com.untangle.tran.nat.gui;

import com.untangle.gui.util.*;
import com.untangle.gui.transform.*;
import com.untangle.gui.pipeline.MPipelineJPanel;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.gui.widgets.dialogs.*;

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

import com.untangle.mvvm.tran.IPaddr;
import com.untangle.mvvm.tran.firewall.ip.IPDBMatcher;
import com.untangle.tran.nat.*;
import com.untangle.mvvm.networking.*;
import com.untangle.mvvm.client.MvvmRemoteContextFactory;

public class MTransformControlsJPanel extends com.untangle.gui.transform.MTransformControlsJPanel {

    private static final String NAME_NET_SPACES       = "Net Spaces";
    private static final String NAME_INTERFACE_MAP    = "Interface-To-Space Map";
    private static final String NAME_SPACE_LIST       = "Space List";
    private static final String NAME_SPACE            = "Space";
    private static final String NAME_ROUTING          = "Routing";
    private static final String NAME_NAT              = "NAT";
    private static final String NAME_DMZ              = "DMZ Host";
    private static final String NAME_REDIRECT         = "Redirect";
    private static final String NAME_REDIRECT_ANY     = "To Anywhere";
    private static final String NAME_REDIRECT_VIRTUAL = "Port Forwarding";
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
        
    private boolean baseGuiBuilt = false;

    private static Nat natTransform;
    public static Nat getNatTransform(){ return natTransform; }
    private SetupState currentSetupState;
    private SetupState previousSetupState;

    private JTabbedPane spacesJTabbedPane;
    private List<NetworkSpace> networkSpaceList;    
    private List<String> spaceNameList = new ArrayList<String>();
    private List<Component> spaceScrollableList = new ArrayList<Component>();

    private List<IPDBMatcher> ipDBMatcherList;
    private IPaddr host;
    private boolean dhcpEnabled;
    public IPaddr getHost(){ return host; }
    public boolean getDhcpEnabled(){ return dhcpEnabled; }

    public MTransformControlsJPanel(MTransformJPanel mTransformJPanel) {
        super(mTransformJPanel);
    }

    private void generateSpecificGui(){
	
	if(SetupState.BASIC.equals(currentSetupState)){
	    // REMOVE ADVANCED STUFF IF IT WAS THERE //
	    if(SetupState.ADVANCED.equals(previousSetupState)){
		removeTab(NAME_NET_SPACES);
		removeSavable(NAME_INTERFACE_MAP);
		removeRefreshable(NAME_INTERFACE_MAP);
		removeSavable(NAME_SPACE_LIST);
		removeRefreshable(NAME_SPACE_LIST);
		for( String name : spaceNameList ){
		    removeSavable(name);
		    removeRefreshable(name);
		}
		spaceNameList.clear();
		removeTab(NAME_ROUTING);
		removeSavable(NAME_ROUTING);
		removeRefreshable(NAME_ROUTING);
	    }

	    // ADD BASIC STUFF IF WE WERENT PREVIOUSLY BASIC
	    if(!SetupState.BASIC.equals(previousSetupState)){
		// NAT ///////////////
		NatJPanel natJPanel = new NatJPanel(this);
		addScrollableTab(0, null, NAME_NAT, null, natJPanel, false, true);
		addSavable(NAME_NAT, natJPanel);
		addRefreshable(NAME_NAT, natJPanel);
		natJPanel.setSettingsChangedListener(this);
		// DMZ ////////////////
		DmzJPanel dmzJPanel = new DmzJPanel();
		addScrollableTab(1, null, NAME_DMZ, null, dmzJPanel, false, true);
		addSavable(NAME_DMZ, dmzJPanel);
		addRefreshable(NAME_DMZ, dmzJPanel);
		dmzJPanel.setSettingsChangedListener(this);
	    }
	}
	else if(SetupState.ADVANCED.equals(currentSetupState)){
	    // REMOVE BASIC STUFF IF IT WAS THERE //
	    if(SetupState.BASIC.equals(previousSetupState)){
		removeTab(NAME_NAT);
		removeSavable(NAME_NAT);
		removeRefreshable(NAME_NAT);
		removeTab(NAME_DMZ);
		removeSavable(NAME_DMZ);
		removeRefreshable(NAME_DMZ);
	    }

	    // ADD ADVANCED STUFF IF WE WERENT PREVIOUSLY ADVANCED
	    if(!SetupState.ADVANCED.equals(previousSetupState)){
		// NET SPACES
		spacesJTabbedPane = addTabbedPane(0, NAME_NET_SPACES, null);
		// INTERFACE MAP //
		InterfaceMapJPanel interfaceMapJPanel = new InterfaceMapJPanel();
		spacesJTabbedPane.addTab(NAME_INTERFACE_MAP, null, interfaceMapJPanel);
		interfaceMapJPanel.setSettingsChangedListener(this);
		addSavable(NAME_INTERFACE_MAP, interfaceMapJPanel);
		addRefreshable(NAME_INTERFACE_MAP, interfaceMapJPanel);
		// SPACE LIST //
		SpaceListJPanel spaceListJPanel = new SpaceListJPanel();
		spacesJTabbedPane.addTab(NAME_SPACE_LIST, null, spaceListJPanel);
		spaceListJPanel.setSettingsChangedListener(this);
		addSavable(NAME_SPACE_LIST, spaceListJPanel);
		addRefreshable(NAME_SPACE_LIST, spaceListJPanel);
		// ROUTING //
		RoutingJPanel routingJPanel = new RoutingJPanel();
		addTab(1, NAME_ROUTING, null, routingJPanel);
		addSavable(NAME_ROUTING, routingJPanel);
		addRefreshable(NAME_ROUTING, routingJPanel);
		routingJPanel.setSettingsChangedListener(this);
	    }

	    // REMOVE PREVIOUS SPACES //
	    for( String name : spaceNameList ){
		removeSavable(name);
		removeRefreshable(name);
	    }
	    spaceNameList.clear();
	    for( Component component : spaceScrollableList ){
		spacesJTabbedPane.remove(component);
	    }
	    spaceScrollableList.clear();

	    // ADD NEW SPACES //
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


	}
	else{
	    // SOME BAD SHITE HAPPENED
	}

    }

    public void generateGui(){

	// GENERATE SPECIFIC GUI //
	generateSpecificGui();

        // REDIRECT /////////////
	JTabbedPane redirectJTabbedPane = addTabbedPane(NAME_REDIRECT, null);

	// REDIRECT VIRTUAL /////////
        RedirectVirtualJPanel redirectVirtualJPanel = new RedirectVirtualJPanel(this);
        redirectJTabbedPane.addTab(NAME_REDIRECT_VIRTUAL, null, redirectVirtualJPanel);
        addSavable(NAME_REDIRECT_VIRTUAL, redirectVirtualJPanel);
	addRefreshable(NAME_REDIRECT_VIRTUAL, redirectVirtualJPanel);
	redirectVirtualJPanel.setSettingsChangedListener(this);

	// REDIRECT ANY /////////
        RedirectJPanel redirectJPanel = new RedirectJPanel();
        redirectJTabbedPane.addTab(NAME_REDIRECT_ANY, null, redirectJPanel);
        addSavable(NAME_REDIRECT_ANY, redirectJPanel);
	addRefreshable(NAME_REDIRECT_ANY, redirectJPanel);
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

    public boolean shouldSave(){
	return new SaveProceedDialog( mTransformJPanel.getTransformDesc().getDisplayName() ).isProceeding();
    }

    public List<IPDBMatcher> getLocalMatcherList(){
	return ipDBMatcherList;
    }

    public void saveAll() throws Exception {
	int previousTimeout = MvvmRemoteContextFactory.factory().getTimeout();
        
        /* Load the current networking configuration, this is used in validation, this is here
         * because it is a remote call. [RBS, per recommendation of inieves] */
        BasicNetworkSettings basicNetworkSettings = Util.getNetworkManager().getBasicSettings();
        ((NatCommonSettings)settings).setNetworkSettings(basicNetworkSettings);
        
	MvvmRemoteContextFactory.factory().setTimeout(Util.RECONFIGURE_NETWORK_TIMEOUT_MILLIS);		
	super.saveAll();
	MvvmRemoteContextFactory.factory().setTimeout(previousTimeout);		
    }

    public void refreshAll() throws Exception {
	BasicNetworkSettings basicNetworkSettings = Util.getNetworkManager().getBasicSettings();
	host = basicNetworkSettings.host();
	dhcpEnabled = basicNetworkSettings.isDhcpEnabled();
	super.refreshAll();
	ipDBMatcherList = ((NatCommonSettings)settings).getLocalMatcherList();
	previousSetupState = currentSetupState;
	currentSetupState = ((NatCommonSettings)settings).getSetupState();
	natTransform = (Nat) mTransformJPanel.getTransform();
	if(SetupState.ADVANCED.equals( currentSetupState )){
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
