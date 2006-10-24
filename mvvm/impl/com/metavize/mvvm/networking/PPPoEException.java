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

package com.metavize.mvvm.networking;

import com.metavize.mvvm.MvvmException;

public class PPPoEException extends NetworkException
{
    public PPPoEException() 
    { 
        super(); 
    }

    public PPPoEException(String message) 
    { 
        super(message); 
    }

    public PPPoEException(String message, Throwable cause) 
    { 
        super(message, cause); 
    }

    public PPPoEException(Throwable cause) 
    { 
        super(cause); 
    }
}
