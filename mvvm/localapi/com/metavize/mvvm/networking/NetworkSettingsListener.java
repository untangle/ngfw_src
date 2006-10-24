/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle Networks, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.networking;

import com.metavize.mvvm.networking.internal.NetworkSpacesInternalSettings;

/* Interface for monitoring changes to the Network Settings */
public interface NetworkSettingsListener
{
    public void event( NetworkSpacesInternalSettings settings );
}
