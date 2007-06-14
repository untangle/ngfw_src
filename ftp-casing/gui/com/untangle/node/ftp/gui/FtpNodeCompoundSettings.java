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

package com.untangle.node.ftp.gui;

import java.awt.Component;

import com.untangle.gui.node.CompoundSettings;
import com.untangle.gui.util.Util;
import com.untangle.node.ftp.FtpSettings;
import com.untangle.node.ftp.FtpNode;

public class FtpNodeCompoundSettings implements CompoundSettings {

    // FTP NODE SETTINGS //
    private FtpSettings ftpNodeSettings;
    public FtpSettings getFtpNodeSettings(){ return ftpNodeSettings; }
    private FtpNode ftpNode;

    // GENERAL SETTINGS //
    private Component generalSettingsComponent;
    public Component getGeneralSettingsComponent(){ return generalSettingsComponent; }

    public void save() throws Exception {
        ftpNode.setFtpSettings(ftpNodeSettings);
    }

    public void refresh() throws Exception {
        if(ftpNode == null)
            ftpNode = (FtpNode) Util.getNode("ftp-casing");
        ftpNodeSettings = (FtpSettings) ftpNode.getFtpSettings();

        if(generalSettingsComponent == null)
            generalSettingsComponent = Util.getSettingsComponent("com.untangle.node.ftp.gui.MCasingJPanel", "ftp-casing");
    }

    public void validate() throws Exception {

    }

}
