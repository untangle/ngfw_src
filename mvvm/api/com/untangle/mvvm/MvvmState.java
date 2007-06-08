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

package com.untangle.mvvm;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Run state of the system.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class MvvmState implements Serializable
{
    private static final long serialVersionUID = -101624240450519097L;

    /**
     * Booted, but not initialized. This is a transient state, just
     * after the world has been instantiated, but before
     * MvvmLocalContext.init() has been called.
     */
    public static final MvvmState LOADED;

    /**
     * Initialized, but not running. We've run init() but not yet started
     * the transforms or Tomcat.
     */
    public static final MvvmState INITIALIZED;

    /**
     * Running.
     */
    public static final MvvmState RUNNING;

    /**
     * Destroyed, this instance should not be used.
     */
    public static final MvvmState DESTROYED;

    private static final Map<String, MvvmState> INSTANCES;

    static {
        LOADED = new MvvmState("loaded");
        INITIALIZED = new MvvmState("initialized");
        RUNNING = new MvvmState("running");
        DESTROYED = new MvvmState("destroyed");

        Map<String, MvvmState> m = new HashMap<String, MvvmState>(4);
        m.put(LOADED.toString(), LOADED);
        m.put(INITIALIZED.toString(), INITIALIZED);
        m.put(RUNNING.toString(), RUNNING);
        m.put(DESTROYED.toString(), DESTROYED);

        INSTANCES = Collections.unmodifiableMap(m);
    }

    private final String state;

    public static MvvmState getInstance(String state)
    {
        return (MvvmState)INSTANCES.get(state);
    }

    private MvvmState(String state) { this.state = state; }

    public String toString() { return state; }

    // Serialization ------------------------------------------------------

    Object readResolve()
    {
        return getInstance(state);
    }
}
