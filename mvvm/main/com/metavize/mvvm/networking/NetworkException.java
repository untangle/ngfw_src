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

package com.metavize.mvvm.networking;

import com.metavize.mvvm.MvvmException;

public class NetworkException extends MvvmException {
    public NetworkException() 
    { 
        super(); 
    }

    public NetworkException(String message) 
    { 
        super(message); 
    }

    public NetworkException(String message, Throwable cause) 
    { 
        super(message, cause); 
    }

    public NetworkException(Throwable cause) 
    { 
        super(cause); 
    }
}
