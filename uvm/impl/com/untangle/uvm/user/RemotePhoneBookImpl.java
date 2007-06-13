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

import com.untangle.uvm.node.ValidateException;

public class RemotePhoneBookImpl implements RemotePhoneBook
{
    private final LocalPhoneBook local;
    
    public RemotePhoneBookImpl( LocalPhoneBook local )
    {
        this.local = local;
    }
                         
    /* retrieve the WMI settings */
    public WMISettings getWMISettings()
    {
        return local.getWMISettings();
    }
    
    /* set the WMI settings */
    public void setWMISettings( WMISettings settings ) throws ValidateException
    {
        local.setWMISettings( settings );
    }

    public String productIdentifier()
    {
        return local.productIdentifier();
    }
}
