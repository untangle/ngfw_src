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


package com.untangle.node.nat.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.util.*;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.untangle.gui.node.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.uvm.client.UvmRemoteContextFactory;
import com.untangle.uvm.networking.*;
import com.untangle.uvm.node.IPaddr;
import com.untangle.uvm.node.firewall.ip.IPDBMatcher;
import com.untangle.node.nat.*;

public class MNodeControlsJPanel extends com.untangle.gui.node.MNodeControlsJPanel {

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

    private static Nat natNode;
    public static Nat getNatNode(){ return natNode; }
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

    public MNodeControlsJPanel(MNodeJPanel mNodeJPanel) {
        super(mNodeJPanel);
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
                SpaceInterfaceMapJPanel interfaceMapJPanel = new SpaceInterfaceMapJPanel();
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
                SpaceRoutingJPanel routingJPanel = new SpaceRoutingJPanel();
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
                    SpacePrimaryJPanel primarySpaceJPanel = new SpacePrimaryJPanel(networkSpace);
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
        DhcpGeneralJPanel dhcpGeneralJPanel = new DhcpGeneralJPanel();
        addScrollableTab(dhcpJTabbedPane, NAME_DHCP + " " + NAME_DHCP_SETTINGS, null, dhcpGeneralJPanel, false, true);
        addSavable(NAME_DHCP + " " + NAME_DHCP_SETTINGS, dhcpGeneralJPanel);
        addRefreshable(NAME_DHCP + " " + NAME_DHCP_SETTINGS, dhcpGeneralJPanel);
        dhcpGeneralJPanel.setSettingsChangedListener(this);

        // DHCP ADDRESSES /////
        DhcpAddressJPanel dhcpAddressJPanel = new DhcpAddressJPanel();
        dhcpAddressJPanel.setMNodeJPanel(mNodeJPanel);
        dhcpJTabbedPane.addTab(NAME_DHCP + " " + NAME_DHCP_ADDRESS_MAP, null, dhcpAddressJPanel );
        addSavable(NAME_DHCP + " " + NAME_DHCP_ADDRESS_MAP, dhcpAddressJPanel);
        addRefreshable(NAME_DHCP + " " + NAME_DHCP_ADDRESS_MAP, dhcpAddressJPanel);
        dhcpAddressJPanel.setSettingsChangedListener(this);

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
        LogJPanel logJPanel = new LogJPanel(mNodeJPanel.getNode(), this);
        addTab(NAME_LOG, null, logJPanel);
        addShutdownable(NAME_LOG, logJPanel);

        baseGuiBuilt = true;
    }

    public boolean shouldSave(){
        return new SaveProceedDialog( mNodeJPanel.getNodeDesc().getDisplayName() ).isProceeding();
    }

    public List<IPDBMatcher> getLocalMatcherList(){
        return ipDBMatcherList;
    }

    public void saveAll() throws Exception {
        int previousTimeout = UvmRemoteContextFactory.factory().getTimeout();

        /* Load the current networking configuration, this is used in validation, this is here
         * because it is a remote call. [RBS, per recommendation of inieves] */
        BasicNetworkSettings basicNetworkSettings = Util.getNetworkManager().getBasicSettings();
        ((NatCommonSettings)settings).setNetworkSettings(basicNetworkSettings);

        UvmRemoteContextFactory.factory().setTimeout(Util.RECONFIGURE_NETWORK_TIMEOUT_MILLIS);
        super.saveAll();
        UvmRemoteContextFactory.factory().setTimeout(previousTimeout);
    }

    public void refreshAll() throws Exception {
        BasicNetworkSettings basicNetworkSettings = Util.getNetworkManager().getBasicSettings();
        host = basicNetworkSettings.host();
        dhcpEnabled = basicNetworkSettings.isDhcpEnabled();
        super.refreshAll();
        ipDBMatcherList = ((NatCommonSettings)settings).getLocalMatcherList();
        previousSetupState = currentSetupState;
        currentSetupState = ((NatCommonSettings)settings).getSetupState();
        natNode = (Nat) mNodeJPanel.getNode();
        if(SetupState.ADVANCED.equals( currentSetupState )){
            networkSpaceList = ((NetworkSpacesSettings)settings).getNetworkSpaceList();
        }
        if( baseGuiBuilt ){
            SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
                int selectedIndex = MNodeControlsJPanel.this.getMTabbedPane().getSelectedIndex();
                String selectedTitle = MNodeControlsJPanel.this.getMTabbedPane().getTitleAt(selectedIndex);
                Component selectedComponent = MNodeControlsJPanel.this.getMTabbedPane().getSelectedComponent();
                JTabbedPane selectedJTabbedPane = null;
                int subSelectedIndex = -1;
                String subSelectedTitle = null;
                if( selectedComponent instanceof JTabbedPane ){
                    selectedJTabbedPane = (JTabbedPane) selectedComponent;
                    subSelectedIndex = selectedJTabbedPane.getSelectedIndex();
                    subSelectedTitle = selectedJTabbedPane.getTitleAt(subSelectedIndex);
                }
                generateSpecificGui();
                int newIndex = MNodeControlsJPanel.this.getMTabbedPane().indexOfTab(selectedTitle);
                MNodeControlsJPanel.this.getMTabbedPane().setSelectedIndex(newIndex);
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
