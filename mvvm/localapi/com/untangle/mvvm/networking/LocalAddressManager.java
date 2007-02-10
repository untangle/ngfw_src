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

package com.untangle.mvvm.networking;

import com.untangle.mvvm.networking.internal.AddressSettingsInternal;

public interface LocalAddressManager
{
    /* Use this to retrieve just the remote settings */
    public AddressSettings getSettings();

    public AddressSettingsInternal getInternalSettings();
    
    /* Use this to mess with the remote settings without modifying the network settings */
    public void setSettings( AddressSettings settings );
}
