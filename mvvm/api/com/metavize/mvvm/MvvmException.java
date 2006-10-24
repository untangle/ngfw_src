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

package com.metavize.mvvm;

public class MvvmException extends Exception {
    public MvvmException() 
    { 
        super(); 
    }

    public MvvmException(String message) 
    { 
        super(message); 
    }

    public MvvmException(String message, Throwable cause) 
    { 
        super(message, cause); 
    }

    public MvvmException(Throwable cause) 
    { 
        super(cause); 
    }
}
