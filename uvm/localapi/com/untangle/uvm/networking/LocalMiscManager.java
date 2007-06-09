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

package com.untangle.uvm.networking;

import com.untangle.uvm.networking.internal.MiscSettingsInternal;

public interface LocalMiscManager
{
    /* Use this to retrieve just the remote settings */
    public MiscSettings getSettings();

    public MiscSettingsInternal getInternalSettings();
    
    /* Use this to mess with the remote settings without modifying the network settings */
    public void setSettings( MiscSettings settings );
}
