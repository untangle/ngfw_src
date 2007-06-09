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

public class NodeStartException extends NodeException
{
    public NodeStartException()
    {
        super();
    }

    public NodeStartException(String message)
    {
        super(message);
    }

    public NodeStartException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public NodeStartException(Throwable cause)
    {
        super(cause);
    }
}
