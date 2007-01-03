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

public class TransformStartException extends TransformException 
{
    public TransformStartException() 
    { 
	super(); 
    }

    public TransformStartException(String message) 
    { 
	super(message); 
    }

    public TransformStartException(String message, Throwable cause) 
    { 
	super(message, cause); 
    }

    public TransformStartException(Throwable cause) 
    { 
	super(cause); 
    }
}
