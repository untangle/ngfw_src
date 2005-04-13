/*
 * Copyright (c) 2003, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: Argon.java 194 2005-04-06 19:13:55Z rbscott $
 */

package com.metavize.mvvm.argon;

import java.net.InetAddress;

import com.metavize.jnetcap.Shield;
import com.metavize.mvvm.ArgonManager;

public class ArgonManagerImpl implements ArgonManager
{    
    private static final Shield shield = Shield.getInstance();

    private static final ArgonManagerImpl INSTANCE = new ArgonManagerImpl();
    
    private ArgonManagerImpl()
    {
    }

    public void shieldStatus( InetAddress ip, int port )
    {
        if ( port < 0 || port > 0xFFFF ) {
            throw new IllegalArgumentException( "Invalid port: " + port );
        }
        shield.status( ip, port );
    }

    public void shieldReconfigure()
    {
        if ( Argon.shieldFile != null ) {
            shield.config( Argon.shieldFile );
        }
    }

    public static final ArgonManager getInstance()
    {
        return INSTANCE;
    }
}
