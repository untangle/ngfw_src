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

public class DownloadException extends TransformException
{
    DownloadException( String message )
    {
        super( message );
    }

    DownloadException( Exception e )
    {
        super( e );
    }

    DownloadException( String message, Exception e )
    {
        super( message, e );
    }

}
