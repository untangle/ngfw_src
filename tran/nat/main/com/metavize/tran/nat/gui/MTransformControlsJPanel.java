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

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;
import com.metavize.gui.widgets.dialogs.*;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.Vector;
import javax.swing.event.*;
import javax.swing.border.*;

import com.metavize.tran.nat.*;

public class MTransformControlsJPanel extends com.metavize.gui.transform.MTransformControlsJPanel{

    private static final String NAME_NAT = "NAT";
    private static final String NAME_DHCP = "DHCP";
    private static final String NAME_DHCP_SETTINGS = "Settings";
    private static final String NAME_DHCP_ADDRESS_MAP = "Address Map";
    private static final String NAME_REDIRECT = "Redirect";
    private static final String NAME_DMZ = "DMZ Host";
    private static final String NAME_DNS = "DNS Forwarding";
    private static final String NAME_DNS_FORWARDING = "Settings";
    private static final String NAME_DNS_ADDRESS_MAP = "Address Map";
    private static final String NAME_LOG = "Event Log";

    protected Dimension MIN_SIZE = new Dimension(640, 480);
    protected Dimension MAX_SIZE = new Dimension(640, 1200);
        
    public MTransformControlsJPanel(MTransformJPanel mTransformJPanel) {
        super(mTransformJPanel);
    }

    protected void generateGui(){

        // NAT ///////////////
        NatJPanel natJPanel = new NatJPanel();
        JScrollPane natJScrollPane = new JScrollPane( natJPanel );
        natJScrollPane.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
        natJScrollPane.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );
        super.mTabbedPane.addTab(NAME_NAT, null, natJScrollPane );
	super.savableMap.put(NAME_NAT, natJPanel);
	super.refreshableMap.put(NAME_NAT, natJPanel);
        
        // DHCP /////////////
        JTabbedPane dhcpJTabbedPane = new JTabbedPane();
        dhcpJTabbedPane.setBorder(new EmptyBorder(7, 13, 13, 13));
        dhcpJTabbedPane.setFocusable(false);
        dhcpJTabbedPane.setFont(new java.awt.Font("Arial", 0, 11));
        dhcpJTabbedPane.setRequestFocusEnabled(false);
	super.mTabbedPane.addTab(NAME_DHCP, null, dhcpJTabbedPane );

	// DHCP SETTINGS /////
        DhcpJPanel dhcpJPanel = new DhcpJPanel();
        JScrollPane dhcpJScrollPane = new JScrollPane( dhcpJPanel );
        dhcpJScrollPane.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
        dhcpJScrollPane.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );
        dhcpJTabbedPane.addTab(NAME_DHCP_SETTINGS, null, dhcpJScrollPane );
        super.savableMap.put(NAME_DHCP + " " + NAME_DHCP_SETTINGS, dhcpJPanel);
        super.refreshableMap.put(NAME_DHCP + " " + NAME_DHCP_SETTINGS, dhcpJPanel);

	// DHCP ADDRESSES /////
	AddressJPanel addressJPanel = new AddressJPanel();
        dhcpJTabbedPane.addTab(NAME_DHCP_ADDRESS_MAP, null, addressJPanel );
        super.savableMap.put(NAME_DHCP + " " + NAME_DHCP_ADDRESS_MAP, addressJPanel);
        super.refreshableMap.put(NAME_DHCP + " " + NAME_DHCP_ADDRESS_MAP, addressJPanel);

        // REDIRECT /////////////
        RedirectJPanel redirectJPanel = new RedirectJPanel();
        super.mTabbedPane.addTab(NAME_REDIRECT, null, redirectJPanel );
        super.savableMap.put(NAME_REDIRECT, redirectJPanel);
	super.refreshableMap.put(NAME_REDIRECT, redirectJPanel);

        // DMZ ////////////////
        DmzJPanel dmzJPanel = new DmzJPanel();
        JScrollPane dmzJScrollPane = new JScrollPane( dmzJPanel );
        dmzJScrollPane.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
        dmzJScrollPane.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );
        super.mTabbedPane.addTab(NAME_DMZ, null, dmzJScrollPane );
	super.savableMap.put(NAME_DMZ, dmzJPanel);
	super.refreshableMap.put(NAME_DMZ, dmzJPanel);

        // DNS /////////////
        JTabbedPane dnsJTabbedPane = new JTabbedPane();
        dnsJTabbedPane.setBorder(new EmptyBorder(7, 13, 13, 13));
        dnsJTabbedPane.setFocusable(false);
        dnsJTabbedPane.setFont(new java.awt.Font("Arial", 0, 11));
        dnsJTabbedPane.setRequestFocusEnabled(false);
	super.mTabbedPane.addTab(NAME_DNS, null, dnsJTabbedPane );

        // DNS SETTINGS /////////////
        DnsJPanel dnsJPanel = new DnsJPanel();
        JScrollPane dnsJScrollPane = new JScrollPane( dnsJPanel );
        dnsJScrollPane.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
        dnsJScrollPane.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );
        dnsJTabbedPane.addTab(NAME_DNS_FORWARDING, null, dnsJScrollPane );
	super.savableMap.put(NAME_DNS + " " + NAME_DNS_FORWARDING, dnsJPanel);
	super.refreshableMap.put(NAME_DNS + " " + NAME_DNS_FORWARDING, dnsJPanel);

	// DNS HOSTS /////
	DnsAddressJPanel dnsAddressJPanel = new DnsAddressJPanel();
        dnsJTabbedPane.addTab(NAME_DNS_ADDRESS_MAP, null, dnsAddressJPanel );
        super.savableMap.put(NAME_DNS + " " + NAME_DNS_ADDRESS_MAP, dnsAddressJPanel);
        super.refreshableMap.put(NAME_DNS + " " + NAME_DNS_ADDRESS_MAP, dnsAddressJPanel);

        LogJPanel logJPanel = new LogJPanel(mTransformJPanel.getTransformContext().transform(), this);
        super.mTabbedPane.addTab( NAME_LOG, null, logJPanel );
    }
        
}


