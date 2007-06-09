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



package com.untangle.node.spyware.gui;


import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.untangle.gui.node.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.node.spyware.*;

public class MNodeControlsJPanel extends com.untangle.gui.node.MNodeControlsJPanel{

    private static final String NAME_BLOCK = "Block Lists";
    private static final String NAME_BLOCK_ACTIVEX = "ActiveX List";
    private static final String NAME_BLOCK_SUBNET = "Subnet List";
    private static final String NAME_BLOCK_COOKIE = "Cookie List";
    private static final String NAME_BLOCK_URL = "URL List";
    private static final String NAME_PASS_DOMAIN = "Pass List";
    private static final String NAME_SETTINGS = "General Settings";
    private static final String NAME_LOG = "Event Log";

    public MNodeControlsJPanel(MNodeJPanel mNodeJPanel) {
        super(mNodeJPanel);
    }

    public void generateGui(){
        // BLOCK LISTS ///////////
        JTabbedPane blockJTabbedPane = addTabbedPane(NAME_BLOCK, null);

        // URL ///////////////
        UrlConfigJPanel urlConfigJPanel = new UrlConfigJPanel();
        blockJTabbedPane.addTab(NAME_BLOCK_URL, null, urlConfigJPanel);
        addSavable(NAME_BLOCK + " " + NAME_BLOCK_URL, urlConfigJPanel);
        addRefreshable(NAME_BLOCK + " " + NAME_BLOCK_URL, urlConfigJPanel);
        urlConfigJPanel.setSettingsChangedListener(this);

        // SUBNETS ///////////////
        SubnetConfigJPanel subnetConfigJPanel = new SubnetConfigJPanel();
        blockJTabbedPane.addTab(NAME_BLOCK_SUBNET, null, subnetConfigJPanel);
        addSavable(NAME_BLOCK + " " + NAME_BLOCK_SUBNET, subnetConfigJPanel);
        addRefreshable(NAME_BLOCK + " " + NAME_BLOCK_SUBNET, subnetConfigJPanel);
        subnetConfigJPanel.setSettingsChangedListener(this);

        // COOKIES //////////////
        CookieConfigJPanel cookieConfigJPanel = new CookieConfigJPanel();
        blockJTabbedPane.addTab(NAME_BLOCK_COOKIE, null, cookieConfigJPanel);
        addSavable(NAME_BLOCK + " " + NAME_BLOCK_COOKIE, cookieConfigJPanel);
        addRefreshable(NAME_BLOCK + " " + NAME_BLOCK_COOKIE, cookieConfigJPanel);
        cookieConfigJPanel.setSettingsChangedListener(this);

        // ACTIVEX ///////////////
        ActiveXConfigJPanel activeXConfigJPanel = new ActiveXConfigJPanel();
        blockJTabbedPane.addTab(NAME_BLOCK_ACTIVEX, null, activeXConfigJPanel);
        addSavable(NAME_BLOCK + " " + NAME_BLOCK_ACTIVEX, activeXConfigJPanel);
        addRefreshable(NAME_BLOCK + " " + NAME_BLOCK_ACTIVEX, activeXConfigJPanel);
        activeXConfigJPanel.setSettingsChangedListener(this);

        // PASS DOMAIN //////////////
        PassDomainConfigJPanel passDomainConfigJPanel = new PassDomainConfigJPanel();
        addTab(NAME_PASS_DOMAIN, null, passDomainConfigJPanel);
        addSavable(NAME_PASS_DOMAIN, passDomainConfigJPanel);
        addRefreshable(NAME_PASS_DOMAIN, passDomainConfigJPanel);
        passDomainConfigJPanel.setSettingsChangedListener(this);

        // GENERAL SETTINGS ////////
        GeneralConfigJPanel generalConfigJPanel = new GeneralConfigJPanel();
        addTab(NAME_SETTINGS, null, generalConfigJPanel);
        addSavable(NAME_SETTINGS, generalConfigJPanel);
        addRefreshable(NAME_SETTINGS, generalConfigJPanel);
        generalConfigJPanel.setSettingsChangedListener(this);

        // EVENT LOG ///////
        LogJPanel logJPanel = new LogJPanel(mNodeJPanel.getNode(), this);
        addTab(NAME_LOG, null, logJPanel);
        addShutdownable(NAME_LOG, logJPanel);
    }
}


