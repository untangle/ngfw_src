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

public class UsbUnavailableException extends NodeException
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
