/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.mvvm.networking;

import com.metavize.mvvm.networking.internal.RemoteInternalSettings;

/* Interface for monitoring changes to the remote settings */
public interface RemoteSettingsListener
{
    public void event( RemoteInternalSettings settings );
}
