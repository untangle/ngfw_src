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

package com.untangle.uvm.argon;

/* A node interface is an interface that can be added by a node.
 * presently this only exists for VPN.
 */
class NodeInterface
{
    private final byte   argonIntf;
    private final String deviceName;
    
    NodeInterface( byte argonIntf, String deviceName )
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
