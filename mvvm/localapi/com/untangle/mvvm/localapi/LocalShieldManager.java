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

package com.untangle.mvvm.localapi;

import java.util.List;

import com.untangle.mvvm.ArgonException;
import com.untangle.mvvm.tran.RemoteShieldManager;
import com.untangle.mvvm.shield.ShieldNodeSettings;

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

