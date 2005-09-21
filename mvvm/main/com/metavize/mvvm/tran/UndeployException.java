/*
 * Copyright (c) 2003, 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.mvvm.tran;

import com.metavize.mvvm.MvvmException;

public class UndeployException extends MvvmException {
    public UndeployException() { super(); }
    public UndeployException(String message) { super(message); }
    public UndeployException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public UndeployException(Throwable cause) { super(cause); }
}
