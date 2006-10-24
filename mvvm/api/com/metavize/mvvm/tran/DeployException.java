/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.tran;

import com.metavize.mvvm.MvvmException;

// relates to the creation of instances
// XXX rename?
public class DeployException extends MvvmException {
    public DeployException() { super(); }
    public DeployException(String message) { super(message); }
    public DeployException(String message, Throwable cause) { super(message, cause); }
    public DeployException(Throwable cause) { super(cause); }
}
