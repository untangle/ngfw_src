/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle Networks, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.toolbox;

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
