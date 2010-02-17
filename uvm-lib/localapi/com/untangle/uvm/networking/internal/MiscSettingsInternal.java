/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.uvm.networking.internal;

import com.untangle.uvm.networking.MiscSettings;

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

