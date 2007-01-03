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

package com.untangle.mvvm.user;

import java.net.InetAddress;

import com.untangle.mvvm.tran.ValidateException;

public interface RemotePhoneBook
{
    /* retrieve the WMI settings */
    public WMISettings getWMISettings();

    /* set the WMI settings */
    public void setWMISettings( WMISettings settings ) throws ValidateException;

    public void wmi( String args[] ) throws Exception;
}

