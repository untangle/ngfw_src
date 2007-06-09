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

package com.untangle.mvvm.policy;

import com.untangle.mvvm.MvvmException;


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
