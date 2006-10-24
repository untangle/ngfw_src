/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle Networks, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.openvpn;

import com.metavize.mvvm.tran.TransformException;

public class UsbUnavailableException extends TransformException
{
    UsbUnavailableException( String message )
    {
        super( message );
    }

    UsbUnavailableException( Exception e )
    {
        super( e );
    }

    UsbUnavailableException( String message, Exception e )
    {
        super( message, e );
    }

}
