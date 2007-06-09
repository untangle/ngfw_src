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

package com.untangle.node.http.gui;

import java.awt.Component;

import com.untangle.gui.node.CompoundSettings;
import com.untangle.gui.util.Util;
import com.untangle.uvm.node.Node;
import com.untangle.node.http.HttpSettings;
import com.untangle.node.http.HttpNode;

public class HttpNodeCompoundSettings implements CompoundSettings {

    // HTTP NODE SETTINGS //
    private HttpSettings httpNodeSettings;
    public HttpSettings getHttpNodeSettings(){ return httpNodeSettings; }
    private HttpNode httpNode;

    // GENERAL SETTINGS //
    private Component generalSettingsComponent;
    public Component getGeneralSettingsComponent(){ return generalSettingsComponent; }

    public void save() throws Exception {
        ((Node)httpNode).setSettings(httpNodeSettings);
    }

    public void refresh() throws Exception {
        if(httpNode == null)
            httpNode = (HttpNode) Util.getNode("http-casing");
        httpNodeSettings = (HttpSettings) ((Node)httpNode).getSettings();

        if(generalSettingsComponent == null)
            generalSettingsComponent = Util.getSettingsComponent("com.untangle.node.http.gui.MCasingJPanel", "http-casing");
    }

    public void validate() throws Exception {

    }

}
