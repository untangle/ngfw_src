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

import com.untangle.uvm.UvmContextFactory;

public class NodeContextSwitcher<T>
{
    private final NodeContext nodeContext;

    public NodeContextSwitcher(NodeContext nodeContext)
    {
        this.nodeContext = nodeContext;
    }

    public void run(Event<T> event, T argument)
    {
        LocalNodeManager tm = UvmContextFactory.context().nodeManager();
        try {
            tm.registerThreadContext(this.nodeContext);
            event.handle(argument);
        } finally {
            tm.deregisterThreadContext();
        }
    }

    public static interface Event<V>
    {
        public void handle(V argument);
    }
}
