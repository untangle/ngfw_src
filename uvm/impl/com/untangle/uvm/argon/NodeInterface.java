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

package com.untangle.mvvm.argon;

/* A transform interface is an interface that can be added by a transform.
 * presently this only exists for VPN.
 */
class TransformInterface
{
    private final byte   argonIntf;
    private final String deviceName;
    
    TransformInterface( byte argonIntf, String deviceName )
    {
        this.argonIntf = argonIntf;
        this.deviceName = deviceName;
    }

    public byte argonIntf()
    {
        return this.argonIntf;
    }

    public String deviceName()
    {
        return this.deviceName;
    }
}
