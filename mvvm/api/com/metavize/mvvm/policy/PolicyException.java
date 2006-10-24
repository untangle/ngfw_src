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

package com.metavize.mvvm.policy;

import com.metavize.mvvm.MvvmException;


public class PolicyException extends MvvmException
{
    public PolicyException()
    {
        super();
    }

    public PolicyException(String message)
    {
        super(message);
    }

    public PolicyException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public PolicyException(Throwable cause)
    {
        super(cause);
    }
}
