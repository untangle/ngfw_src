/*
 * $HeadURL$
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
