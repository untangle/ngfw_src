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

package com.untangle.uvm.engine;

import com.untangle.uvm.tapi.AbstractEventHandler;
import com.untangle.uvm.node.Node;

/**
 * <code>ReleasedEventHandler</code> is a plain vanilla event handler used for released
 * sessions and whenever the node has no smithEventListener.  We just use everything
 * from AbstractEventHandler.
 *
 * @author <a href="mailto:jdi@slab.ninthwave.com">John Irwin</a>
 * @version 1.0
 */
class ReleasedEventHandler extends AbstractEventHandler {
    ReleasedEventHandler(Node node) {
        super(node);
    }
}
