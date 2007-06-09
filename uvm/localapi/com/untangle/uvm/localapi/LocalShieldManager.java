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

package com.untangle.uvm.localapi;

import java.util.List;

import com.untangle.uvm.ArgonException;
import com.untangle.uvm.node.RemoteShieldManager;
import com.untangle.uvm.shield.ShieldNodeSettings;

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

