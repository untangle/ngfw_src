/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.ftp.gui;

import com.metavize.gui.util.Util;
import com.metavize.gui.transform.CompoundSettings;
import com.metavize.tran.ftp.FtpSettings;
import com.metavize.tran.ftp.FtpTransform;

import java.awt.Component;

public class FtpTransformCompoundSettings implements CompoundSettings {

    // FTP TRANSFORM SETTINGS //
    private FtpSettings ftpTransformSettings;
    public FtpSettings getFtpTransformSettings(){ return ftpTransformSettings; }
    private FtpTransform ftpTransform;

    // GENERAL SETTINGS //
    private Component generalSettingsComponent;
    public Component getGeneralSettingsComponent(){ return generalSettingsComponent; }

    public void save() throws Exception {
	ftpTransform.setFtpSettings(ftpTransformSettings);
    }

    public void refresh() throws Exception {
	if(ftpTransform == null)
	    ftpTransform = (FtpTransform) Util.getTransform("ftp-casing");
	ftpTransformSettings = (FtpSettings) ftpTransform.getFtpSettings();

	if(generalSettingsComponent == null)
	    generalSettingsComponent = Util.getSettingsComponent("com.metavize.tran.ftp.gui.MCasingJPanel", "ftp-casing");
    }

    public void validate() throws Exception {

    }

}
