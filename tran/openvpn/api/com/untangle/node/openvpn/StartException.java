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
package com.untangle.tran.openvpn;

import com.untangle.mvvm.tran.TransformException;

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
