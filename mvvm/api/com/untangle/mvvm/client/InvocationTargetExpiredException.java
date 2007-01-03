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

package com.untangle.mvvm.client;

public class InvocationTargetExpiredException extends InvocationException {
    public InvocationTargetExpiredException() { super(); }
    public InvocationTargetExpiredException(String message) { super(message); }
    public InvocationTargetExpiredException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public InvocationTargetExpiredException(Throwable cause) { super(cause); }
}
