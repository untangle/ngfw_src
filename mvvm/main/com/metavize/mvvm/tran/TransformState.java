/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: TransformState.java,v 1.3 2005/02/25 02:45:29 amread Exp $
 */

package com.metavize.mvvm.tran;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents the runtime state of a transform instance.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 */
public class TransformState implements Serializable
{
    private static final long serialVersionUID = -101624240450519097L;

    /**
     * Instantiated, but not initialized. This is a transient state, just
     * after the main transform class has been instantiated, but before
     * init has been called.
     *
     */
    public static final TransformState LOADED = new TransformState("loaded");

    /**
     * Initialized, but not running. The transform instance enters this state
     * after it has been initialized, or when it is stopped.
     *
     */
    public static final TransformState INITIALIZED
        = new TransformState("initialized");

    /**
     * Running.
     *
     */
    public static final TransformState RUNNING = new TransformState("running");

    /**
     * Destroyed, this instance should not be used.
     *
     */
    public static final TransformState DESTROYED
        = new TransformState("destroyed");

    private static final Map INSTANCES = new HashMap();

    static {
        INSTANCES.put(LOADED.toString(), LOADED);
        INSTANCES.put(INITIALIZED.toString(), INITIALIZED);
        INSTANCES.put(RUNNING.toString(), RUNNING);
        INSTANCES.put(DESTROYED.toString(), DESTROYED);
    }

    private String state;

    public static TransformState getInstance(String state)
    {
        return (TransformState)INSTANCES.get(state);
    }

    private TransformState(String state) { this.state = state; }

    public String toString() { return state; }

    // Serialization ----------------------------------------------------------
    Object readResolve()
    {
        return getInstance(state);
    }
}
