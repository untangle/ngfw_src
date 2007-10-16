/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.uvm.node;

import com.untangle.uvm.security.Tid;
import com.untangle.uvm.vnet.IPSessionDesc;

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
