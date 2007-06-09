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

public class NodeStopException extends NodeException
{
    public NodeStopException()
    {
        super();
    }

    public NodeStopException(String message)
    {
        super(message);
    }

    public NodeStopException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public NodeStopException(Throwable cause)
    {
        super(cause);
    }
}
