/*
 * Copyright (c) 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.argon;

import java.net.InetAddress;

import com.metavize.jnetcap.Netcap;

public interface IPSessionDesc extends SessionDesc, SessionEndpoints
{
    public final short IPPROTO_TCP = (short)Netcap.IPPROTO_TCP;
    public final short IPPROTO_UDP = (short)Netcap.IPPROTO_UDP;
}
