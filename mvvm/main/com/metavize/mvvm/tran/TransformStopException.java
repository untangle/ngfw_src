/* 
 * Copyright (c) 2004 Metavize Inc.
 * All rights reserved.
 * 
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 * 
 * $Id: TransformStopException.java,v 1.1.1.1 2004/12/01 23:32:22 amread Exp $
 */

package com.metavize.mvvm.tran;

public class TransformStopException extends TransformException 
{
    public TransformStopException() 
    { 
	super(); 
    }

    public TransformStopException(String message) 
    { 
	super(message); 
    }

    public TransformStopException(String message, Throwable cause) 
    { 
	super(message, cause); 
    }

    public TransformStopException(Throwable cause) 
    { 
	super(cause); 
    }
}
