/*
 * Copyright (c) 2004, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.toolbox;

public class MackageUninstallException extends MackageException
{
    public MackageUninstallException()
    {
        super();
    }

    public MackageUninstallException(String message)
    {
        super(message);
    }

    public MackageUninstallException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public MackageUninstallException(Throwable cause)
    {
        super(cause);
    }
}
