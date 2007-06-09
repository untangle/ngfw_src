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

package com.untangle.uvm.tapi;

import java.util.Arrays;
import java.util.Collections;
import java.util.EventObject;
import java.util.List;

import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.NodeState;

public class NodeStateChangeEvent extends EventObject
{
    private final NodeState nodeState;
    private final List<String> args;

    NodeStateChangeEvent(Node t, NodeState nodeState,
                              String[] args)
    {
        this(t, nodeState, (List<String>)(null == args ? Collections.emptyList() :  Arrays.asList(args)));
    }

    NodeStateChangeEvent(Node t, NodeState nodeState,
                              List<String> args)
    {
        super(t);

        this.nodeState = nodeState;
        this.args = Collections.unmodifiableList(args);
    }

    public NodeState getNodeState()
    {
        return nodeState;
    }

    public List<String> getArgs()
    {
        return args;
    }
}