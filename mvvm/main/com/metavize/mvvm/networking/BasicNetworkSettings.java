/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.mvvm.networking;

import java.util.List;
import java.util.LinkedList;
import java.util.Collections;

import com.metavize.mvvm.tran.IPaddr;

public class BasicNetworkSettings
{
    private final NetworkSettings completeConfiguration;
    
    /* Going back and forth is only allowed inside this package */
    BasicNetworkSettings( NetworkSettings configuration )
    {
        this.completeConfiguration = configuration;
    }
    
    public void setDns1( IPaddr dns1 ) 
    {
        this.completeConfiguration.setDns1( dns1 );
    }

    public IPaddr getDns1() 
    {
        return this.completeConfiguration.getDns1();
    }

    public void setDns2( IPaddr dns2 ) 
    {
        this.completeConfiguration.setDns2( dns2 );
    }

    public IPaddr getDns2() 
    {
        return this.completeConfiguration.getDns2();
    }

    public boolean hasDns2() 
    {
        return this.completeConfiguration.hasDns2();
    }

    public String getHostname()
    {
        return this.completeConfiguration.getHostname();
    }

    public void setHostname( String hostname )
    {
        this.completeConfiguration.setHostname( hostname );
    }

    public String getPublicUrl()
    {
        return this.completeConfiguration.getPublicUrl();
    }

    public void setPublicUrl( String publicUrl )
    {
        this.completeConfiguration.setPublicUrl( publicUrl );
    }

    public boolean hasPublicUrl()
    {
        return this.completeConfiguration.hasPublicUrl();
    }

    /* Going back and forth is only allowed inside this package */
    NetworkSettings getNetworkSettings()
    {
        return this.completeConfiguration;
    }
}
