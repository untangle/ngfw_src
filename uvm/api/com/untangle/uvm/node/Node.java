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

import com.untangle.uvm.security.Tid;
import com.untangle.uvm.tapi.IPSessionDesc;

/**
 * Interface for a node instance, provides public runtime control
 * methods for manipulating the instance's state.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public interface Node
{
    static final int GENERIC_0_COUNTER = 6; // XXX temp hack
    static final int GENERIC_1_COUNTER = 7; // XXX temp hack
    static final int GENERIC_2_COUNTER = 8; // XXX temp hack
    static final int GENERIC_3_COUNTER = 9; // XXX temp hack
    static final int GENERIC_4_COUNTER = 10; // XXX temp hack
    static final int GENERIC_5_COUNTER = 11; // XXX temp hack
    static final int GENERIC_6_COUNTER = 12; // XXX temp hack
    static final int GENERIC_7_COUNTER = 13; // XXX temp hack
    static final int GENERIC_8_COUNTER = 14; // XXX temp hack
    static final int GENERIC_9_COUNTER = 15; // XXX temp hack

    // accessors --------------------------------------------------------------

    public Tid getTid();

    // lifecycle methods ------------------------------------------------------

    /**
     * Get the runtime state of this node instance.
     *
     * @return a <code>NodeState</code> reflecting the runtime
     * state of this node.
     * @see NodeState
     */
    NodeState getRunState();

    /**
     * Tests if the node has ever been started.
     *
     * @return true when the node has never been started.
     */
    boolean neverStarted();

    /**
     * Connects to MetaPipe and starts. The node instance reads its
     * configuration each time this method is called. A call to this method
     * is only valid when the instance is in the
     * {@link NodeState#INITIALIZED} state. After successful return,
     * the instance will be in the {@link NodeState#RUNNING} state.
     *
     * @throws NodeStartException if an exception occurs in start.
     * @exception IllegalStateException if not called in the {@link
     * NodeState#INITIALIZED} state.
     */
    void start() throws NodeStartException, IllegalStateException;

    /**
     * Stops node and disconnects from the MetaPipe. A call to
     * this method is only valid when the instance is in the {@link
     * NodeState#RUNNING} state. After successful return, the
     * instance will be in the {@link NodeState#INITIALIZED}
     * state.
     *
     * @throws NodeStopException if an exception occurs in stop.
     * @exception IllegalStateException if not called in the {@link
     * NodeState#RUNNING} state.
     */
    void stop() throws NodeStopException, IllegalStateException;

    NodeContext getNodeContext();

    NodeDesc getNodeDesc();

    IPSessionDesc[] liveSessionDescs();

    /**
     * <code>dumpSessions</code> dumps the session descriptions in
     * gory detail to the node log.  This is for debugging only.
     */
    void dumpSessions();

    /**
     * Returns the <code>NodeStats</code> for this node.
     * The node must be in the running state.
     *
     * @return a <code>NodeStats</code> giving the
     * <code>NodeStats</code> since the start of the node
     * @exception NodeException if the node is not in the
     * <code>RUNNING</code> state
     */
    NodeStats getStats() throws IllegalStateException;

    // XXX future deprecated methods ------------------------------------------

    Object getSettings();
    void setSettings(Object settings) throws Exception;
}
