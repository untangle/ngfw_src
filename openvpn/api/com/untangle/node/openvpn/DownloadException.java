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
package com.untangle.node.openvpn;

import com.untangle.uvm.node.NodeException;

public class DownloadException extends NodeException
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
