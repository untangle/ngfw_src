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

import com.metavize.mvvm.tran.TransformException;

public class StartException extends TransformException
{
    StartException( String message )
    {
        super( message );
    }

    StartException( Exception e )
    {
        super( e );
    }

    StartException( String message, Exception e )
    {
        super( message, e );
    }

}
