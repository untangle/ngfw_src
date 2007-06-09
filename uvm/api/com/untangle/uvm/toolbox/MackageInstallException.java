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

public class MackageInstallException extends MackageException
{
    public MackageInstallException()
    {
        super();
    }

    public MackageInstallException(String message)
    {
        super(message);
    }

    public MackageInstallException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public MackageInstallException(Throwable cause)
    {
        super(cause);
    }
}
