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

package com.untangle.mvvm.tran;

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
