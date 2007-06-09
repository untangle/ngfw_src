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

package com.untangle.uvm.user;

import java.net.InetAddress;

import com.untangle.uvm.node.ValidateException;

public interface RemotePhoneBook
{
    /**
     * Get the WMI settings
     *
     * @return The new settings.
     */
    public WMISettings getWMISettings();

    /**
     * Set the WMI settings
     *
     * @param settings The new settings.
     * @exception ValidateException if <code>settings</code> is not valid.
     */
    public void setWMISettings( WMISettings settings ) throws ValidateException;
}

