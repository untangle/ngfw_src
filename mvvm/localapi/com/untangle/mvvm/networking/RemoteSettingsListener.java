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

import com.untangle.mvvm.networking.internal.RemoteInternalSettings;

/* Interface for monitoring changes to the remote settings */
public interface RemoteSettingsListener
{
    public void event( RemoteInternalSettings settings );
}
