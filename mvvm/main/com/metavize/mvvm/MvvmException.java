/*
 * Copyright (c) 2003, 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: MvvmException.java,v 1.1.1.1 2004/12/01 23:32:21 amread Exp $
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
