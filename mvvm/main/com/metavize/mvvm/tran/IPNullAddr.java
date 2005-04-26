
/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.mvvm.tran;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

/* This adds the extension that an empty string is parseable as a null IPaddr.
 * This is different from an IPaddr where an empty string is not parseable.
 */
public class IPNullAddr extends IPaddr
{
    private static final long serialVersionUID = -741858749430271001L;
    
    public IPNullAddr( Inet4Address addr )
    {
        super( addr );
    }

    public static IPNullAddr parse( String dotNotation ) throws IllegalArgumentException, UnknownHostException
    {
        /* Trim any whitespace */
        dotNotation = dotNotation.trim();
        
        if ( dotNotation.length() == 0 ) {
            return new IPNullAddr( null );
        } else {
            return new IPNullAddr((Inet4Address)IPaddr.parse( dotNotation ).getAddr());
        }        
    }    
}

