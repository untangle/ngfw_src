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

package com.untangle.mvvm.networking;

import com.untangle.mvvm.MvvmException;

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
