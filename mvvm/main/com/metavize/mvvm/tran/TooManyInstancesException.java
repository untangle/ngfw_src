/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.tran;


// relates to the creation of instances
// XXX rename?
public class TooManyInstancesException extends DeployException {
    public TooManyInstancesException() { super(); }

    public TooManyInstancesException(String message) { super(message); }

    public TooManyInstancesException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public TooManyInstancesException(Throwable cause) { super(cause); }
}
