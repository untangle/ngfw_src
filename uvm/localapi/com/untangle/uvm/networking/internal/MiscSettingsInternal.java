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

package com.untangle.uvm.networking.internal;

import com.untangle.uvm.networking.MiscSettings;

import com.untangle.uvm.node.HostName;
import com.untangle.uvm.node.IPaddr;

/** These are settings related to remote access to the untangle. */
public class MiscSettingsInternal
{
    private final boolean isExceptionReportingEnabled;
    private final boolean isTcpWindowScalingEnabled;
    private final String postConfigurationScript;
    private final String customRules;
    
    private MiscSettingsInternal( MiscSettings settings )
    {
        this.isExceptionReportingEnabled = settings.getIsExceptionReportingEnabled();
        this.isTcpWindowScalingEnabled = settings.getIsTcpWindowScalingEnabled();
        this.postConfigurationScript = settings.getPostConfigurationScript();
        this.customRules = settings.getCustomRules();

    }

    /* Get whether or not exception reporting is enabled */
    public boolean getIsExceptionReportingEnabled()
    {
        return this.isExceptionReportingEnabled;
    }

    /* Get whether tcp window scaling is enabled */
    public boolean getIsTcpWindowScalingEnabled()
    {
        return this.isTcpWindowScalingEnabled;
    }

    /* Get the script to run once the box is configured */
    public String getPostConfigurationScript()
    {
        return this.postConfigurationScript;
    }

    /* Get the post configuration script */
    public String getCustomRules()
    {
        return this.customRules;
    }
    
    public MiscSettings toSettings()
    {
        MiscSettings settings = new MiscSettings();

        settings.setIsExceptionReportingEnabled( getIsExceptionReportingEnabled());        
        settings.setIsTcpWindowScalingEnabled( getIsTcpWindowScalingEnabled());
        settings.setPostConfigurationScript( getPostConfigurationScript());
        settings.setCustomRules( getCustomRules());
        return settings;
    }

    public static MiscSettingsInternal makeInstance( MiscSettings settings )
    {
        
        return new MiscSettingsInternal( settings );
    }
}

