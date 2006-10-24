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

package com.metavize.mvvm.argon;

import java.net.InetAddress;

import com.metavize.jnetcap.Netcap;

public interface IPSessionDesc extends com.metavize.mvvm.api.IPSessionDesc, SessionDesc, SessionEndpoints
{
    public final short IPPROTO_TCP = (short)Netcap.IPPROTO_TCP;
    public final short IPPROTO_UDP = (short)Netcap.IPPROTO_UDP;
}
