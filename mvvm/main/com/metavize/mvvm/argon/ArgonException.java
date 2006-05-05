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

package com.metavize.mvvm.argon;

import com.metavize.mvvm.MvvmException;

public class ArgonException extends MvvmException {
    public ArgonException() 
    { 
        super(); 
    }

    public ArgonException(String message) 
    { 
        super(message); 
    }

    public ArgonException(String message, Throwable cause) 
    { 
        super(message, cause); 
    }

    public ArgonException(Throwable cause) 
    { 
        super(cause); 
    }
}
