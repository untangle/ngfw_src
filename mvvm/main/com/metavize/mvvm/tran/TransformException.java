/* 
 * Copyright (c) 2004 Metavize Inc.
 * All rights reserved.
 * 
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 * 
 * $Id: TransformException.java,v 1.1.1.1 2004/12/01 23:32:22 amread Exp $
 */

package com.metavize.mvvm.tran;

import com.metavize.mvvm.MvvmException;

public class TransformException extends MvvmException 
{
    public TransformException() 
    { 
	super(); 
    }

    public TransformException(String message) 
    { 
	super(message); 
    }

    public TransformException(String message, Throwable cause) 
    { 
	super(message, cause); 
    }

    public TransformException(Throwable cause) 
    { 
	super(cause); 
    }
}

