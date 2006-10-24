/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
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
