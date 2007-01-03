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

package com.untangle.tran.ftp.gui;

import com.untangle.gui.util.Util;
import com.untangle.gui.transform.CompoundSettings;
import com.untangle.tran.ftp.FtpSettings;
import com.untangle.tran.ftp.FtpTransform;

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
	    generalSettingsComponent = Util.getSettingsComponent("com.untangle.tran.ftp.gui.MCasingJPanel", "ftp-casing");
    }

    public void validate() throws Exception {

    }

}
