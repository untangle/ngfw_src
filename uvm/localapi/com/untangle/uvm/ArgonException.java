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

package com.untangle.uvm;

import com.untangle.uvm.UvmException;

public class ArgonException extends UvmException {
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
