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
