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

package com.untangle.uvm.networking;

import com.untangle.uvm.UvmException;

/**
 * Exception for networking errors.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
public class NetworkException extends UvmException
{
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
