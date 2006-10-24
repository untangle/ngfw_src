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

package com.metavize.mvvm.localapi;

import java.util.List;

import com.metavize.mvvm.ArgonException;
import com.metavize.mvvm.api.RemoteShieldManager;
import com.metavize.mvvm.shield.ShieldNodeSettings;

public interface LocalShieldManager extends RemoteShieldManager
{
    /* Toggle whether or not the shield is enabled */
    public void setIsShieldEnabled( boolean isEnabled );

    /* Set the file used to configure the shield */
    public void setShieldConfigurationFile( String file );

    /* Set the shield node rules */
    public void setShieldNodeSettings( List<ShieldNodeSettings> shieldNodeSettingsList ) 
        throws ArgonException;
}

