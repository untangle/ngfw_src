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

package com.untangle.uvm.node;

import com.untangle.uvm.UvmException;

public class UndeployException extends UvmException {
    public UndeployException() { super(); }
    public UndeployException(String message) { super(message); }
    public UndeployException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public UndeployException(Throwable cause) { super(cause); }
}
