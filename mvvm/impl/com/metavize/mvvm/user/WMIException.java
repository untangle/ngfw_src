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

package com.metavize.mvvm.user;

import com.metavize.mvvm.MvvmException;

class WMIException extends MvvmException
{
    public WMIException()
    {
        super();
    }

    public WMIException( String message )
    {
        super( message );
    }

    public WMIException( String message, Throwable cause )
    {
        super( message, cause );
    }

    public WMIException( Throwable cause )
    {
        super( cause );
    }
}