/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: InvocationTargetExpiredException.java,v 1.1 2005/01/30 05:35:59 amread Exp $
 */

package com.metavize.mvvm.client;

public class InvocationTargetExpiredException extends InvocationException {
    public InvocationTargetExpiredException() { super(); }
    public InvocationTargetExpiredException(String message) { super(message); }
    public InvocationTargetExpiredException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public InvocationTargetExpiredException(Throwable cause) { super(cause); }
}
