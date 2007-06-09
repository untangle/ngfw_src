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

import java.io.InputStream;

import com.untangle.uvm.security.Tid;
import com.untangle.uvm.tapi.IPSessionDesc;
import com.untangle.uvm.toolbox.MackageDesc;
import com.untangle.uvm.util.TransactionWork;

/**
 * Holds the context for a Node instance.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public interface NodeContext
{
    /**
     * Get the Tid for this instance.
     *
     * @return the node id.
     */
    Tid getTid();

    /**
     * Get the node for this context.
     *
     * @return this context's node.
     */
    Node node();

    /**
     * Returns desc from uvm-node.xml.
     *
     * @return the NodeDesc.
     */
    NodeDesc getNodeDesc();

    /**
     * Returns the node preferences.
     *
     * @return the NodePreferences.
     */
    NodePreferences getNodePreferences();

    /**
     * Get the {@link MackageDesc} corresponding to this instance.
     *
     * @return the MackageDesc.
     */
    MackageDesc getMackageDesc();

    // XXX should be LocalNodeContext ------------------------------------

    // XXX
    boolean runTransaction(TransactionWork tw);

    InputStream getResourceAsStream(String resource);

    // call-through methods ---------------------------------------------------

    IPSessionDesc[] liveSessionDescs();

    NodeState getRunState();

    NodeStats getStats();
}
