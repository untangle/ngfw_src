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

public class StartException extends NodeException
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
