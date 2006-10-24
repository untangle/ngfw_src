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

package com.metavize.jvector;

public class JVectorException extends Exception
{
    public JVectorException() 
    { 
        super(); 
    }

    public JVectorException( String message )
    { 
        super( message ); 
    }

    public JVectorException( String message, Throwable cause )
    { 
        super( message, cause );
    }

    public JVectorException( Throwable cause )
    { 
        super( cause ); 
    }
}
