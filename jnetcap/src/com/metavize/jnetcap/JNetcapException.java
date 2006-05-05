/*
 * Copyright (c) 2003, 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.jnetcap;

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
