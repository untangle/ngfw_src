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

package com.untangle.mvvm.tran;

import com.untangle.mvvm.security.Tid;
import com.untangle.mvvm.tapi.IPSessionDesc;

/**
 * Interface for a transform instance, provides public runtime control
 * methods for manipulating the instance's state.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public interface Transform
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
     * Get the runtime state of this transform instance.
     *
     * @return a <code>TransformState</code> reflecting the runtime
     * state of this transform.
     * @see TransformState
     */
    TransformState getRunState();

    /**
     * Tests if the transform has ever been started.
     *
     * @return true when the transform has never been started.
     */
    boolean neverStarted();

    /**
     * Connects to MetaPipe and starts. The transform instance reads its
     * configuration each time this method is called. A call to this method
     * is only valid when the instance is in the
     * {@link TransformState#INITIALIZED} state. After successful return,
     * the instance will be in the {@link TransformState#RUNNING} state.
     *
     * @throws TransformStartException if an exception occurs in start.
     * @exception IllegalStateException if not called in the {@link
     * TransformState#INITIALIZED} state.
     */
    void start() throws TransformStartException, IllegalStateException;

    /**
     * Stops transform and disconnects from the MetaPipe. A call to
     * this method is only valid when the instance is in the {@link
     * TransformState#RUNNING} state. After successful return, the
     * instance will be in the {@link TransformState#INITIALIZED}
     * state.
     *
     * @throws TransformStopException if an exception occurs in stop.
     * @exception IllegalStateException if not called in the {@link
     * TransformState#RUNNING} state.
     */
    void stop() throws TransformStopException, IllegalStateException;

    TransformContext getTransformContext();

    TransformDesc getTransformDesc();

    IPSessionDesc[] liveSessionDescs();

    /**
     * <code>dumpSessions</code> dumps the session descriptions in
     * gory detail to the transform log.  This is for debugging only.
     */
    void dumpSessions();

    /**
     * Returns the <code>TransformStats</code> for this transform.
     * The transform must be in the running state.
     *
     * @return a <code>TransformStats</code> giving the
     * <code>TransformStats</code> since the start of the transform
     * @exception TransformException if the transform is not in the
     * <code>RUNNING</code> state
     */
    TransformStats getStats() throws IllegalStateException;

    // XXX future deprecated methods ------------------------------------------

    Object getSettings();
    void setSettings(Object settings) throws Exception;
}
