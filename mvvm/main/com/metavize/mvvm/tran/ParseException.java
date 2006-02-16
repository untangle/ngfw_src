/*
 * Copyright (c) 2003, 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.mvvm.tran;

public class ParseException extends ValidateException
{
    public ParseException() 
    { 
        super(); 
    }

    public ParseException( String message )
    { 
        super( message ); 
    }

    public ParseException( String message, Throwable cause )
    { 
        super( message, cause );
    }

    public ParseException( Throwable cause )
    { 
        super( cause ); 
    }
}
