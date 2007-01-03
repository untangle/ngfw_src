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

package com.untangle.jnetcap;

public class JNetcapException extends Exception
{
    public JNetcapException() 
    { 
        super(); 
    }

    public JNetcapException( String message )
    { 
        super( message ); 
    }

    public JNetcapException( String message, Throwable cause )
    { 
        super( message, cause );
    }

    public JNetcapException( Throwable cause )
    { 
        super( cause ); 
    }
}
