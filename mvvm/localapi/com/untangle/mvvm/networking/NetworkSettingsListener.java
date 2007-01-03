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

import com.untangle.mvvm.networking.internal.NetworkSpacesInternalSettings;

/* Interface for monitoring changes to the Network Settings */
public interface NetworkSettingsListener
{
    public void event( NetworkSpacesInternalSettings settings );
}
