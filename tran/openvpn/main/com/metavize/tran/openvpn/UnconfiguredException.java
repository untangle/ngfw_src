/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.openvpn;

import com.metavize.mvvm.tran.TransformStartException;

public class UnconfiguredException extends TransformStartException
{
    UnconfiguredException( String message )
    {
        super( message );
    }

    UnconfiguredException( Exception e )
    {
        super( e );
    }

    UnconfiguredException( String message, Exception e )
    {
        super( message, e );
    }

}
