/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.client;

public class InvocationException extends RuntimeException {
    public InvocationException() { super(); }
    public InvocationException(String message) { super(message); }
    public InvocationException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public InvocationException(Throwable cause) { super(cause); }
}
