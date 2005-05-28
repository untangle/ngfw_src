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

package com.metavize.mvvm;

public class ValidateException extends Exception
{
    public ValidateException()
    { 
        super(); 
    }

    public ValidateException( String message )
    { 
        super(message); 
    }

    public ValidateException( String message, Throwable cause )
    { 
        super( message, cause );
    }

    public ValidateException( Throwable cause )
    { 
        super( cause );
    }
}
