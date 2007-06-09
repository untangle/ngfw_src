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

package com.untangle.uvm.toolbox;

import com.untangle.uvm.UvmException;

public class MackageException extends UvmException
{
    public MackageException()
    {
        super();
    }

    public MackageException(String message)
    {
        super(message);
    }

    public MackageException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public MackageException(Throwable cause)
    {
        super(cause);
    }
}
