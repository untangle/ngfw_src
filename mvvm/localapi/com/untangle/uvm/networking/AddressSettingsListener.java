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

/* Interface for monitoring changes to the hostname settings. */
public interface AddressSettingsListener
{
    public void event( AddressSettingsInternal settings );
}
