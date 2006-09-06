/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.http.gui;

import com.metavize.gui.util.Util;
import com.metavize.gui.transform.CompoundSettings;
import com.metavize.mvvm.tran.Transform;
import com.metavize.tran.http.HttpTransform;
import com.metavize.tran.http.HttpSettings;

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
	    generalSettingsComponent = Util.getSettingsComponent("com.metavize.tran.http.gui.MCasingJPanel", "http-casing");
    }

    public void validate() throws Exception {

    }

}
