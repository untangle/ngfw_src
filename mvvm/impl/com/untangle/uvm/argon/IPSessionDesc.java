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

import java.net.InetAddress;

import com.untangle.jnetcap.Netcap;

public interface IPSessionDesc extends com.untangle.mvvm.tran.IPSessionDesc, SessionDesc, SessionEndpoints
{
    public final short IPPROTO_TCP = (short)Netcap.IPPROTO_TCP;
    public final short IPPROTO_UDP = (short)Netcap.IPPROTO_UDP;
}
