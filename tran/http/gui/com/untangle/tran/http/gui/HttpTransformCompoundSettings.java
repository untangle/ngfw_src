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

package com.untangle.tran.http.gui;

import com.untangle.gui.util.Util;
import com.untangle.gui.transform.CompoundSettings;
import com.untangle.mvvm.tran.Transform;
import com.untangle.tran.http.HttpTransform;
import com.untangle.tran.http.HttpSettings;

import java.awt.Component;

public class HttpTransformCompoundSettings implements CompoundSettings {

    // HTTP TRANSFORM SETTINGS //
    private HttpSettings httpTransformSettings;
    public HttpSettings getHttpTransformSettings(){ return httpTransformSettings; }
    private HttpTransform httpTransform;

    // GENERAL SETTINGS //
    private Component generalSettingsComponent;
    public Component getGeneralSettingsComponent(){ return generalSettingsComponent; }

    public void save() throws Exception {
	((Transform)httpTransform).setSettings(httpTransformSettings);
    }

    public void refresh() throws Exception {
	if(httpTransform == null)
	    httpTransform = (HttpTransform) Util.getTransform("http-casing");
	httpTransformSettings = (HttpSettings) ((Transform)httpTransform).getSettings();

	if(generalSettingsComponent == null)
	    generalSettingsComponent = Util.getSettingsComponent("com.untangle.tran.http.gui.MCasingJPanel", "http-casing");
    }

    public void validate() throws Exception {

    }

}
