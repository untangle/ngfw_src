/* 
 * Copyright (c) 2004 Metavize Inc.
 * All rights reserved.
 * 
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 * 
 * $Id: MvvmLoginException.java,v 1.1.1.1 2004/12/01 23:32:22 amread Exp $
 */

package com.metavize.mvvm.security;

import javax.security.auth.login.LoginException;

/**
 * Exception for login failures.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 */
public class MvvmLoginException extends LoginException
{    
    public MvvmLoginException() 
    { 
	super(); 
    }

    public MvvmLoginException(String message) 
    { 
	super(message); 
    }
}
