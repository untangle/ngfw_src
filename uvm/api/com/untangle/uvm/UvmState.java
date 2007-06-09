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

package com.untangle.uvm;

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
public class UvmState implements Serializable
{
    private static final long serialVersionUID = -101624240450519097L;

    /**
     * Booted, but not initialized. This is a transient state, just
     * after the world has been instantiated, but before
     * UvmLocalContext.init() has been called.
     */
    public static final UvmState LOADED;

    /**
     * Initialized, but not running. We've run init() but not yet started
     * the nodes or Tomcat.
     */
    public static final UvmState INITIALIZED;

    /**
     * Running.
     */
    public static final UvmState RUNNING;

    /**
     * Destroyed, this instance should not be used.
     */
    public static final UvmState DESTROYED;

    private static final Map<String, UvmState> INSTANCES;

    static {
        LOADED = new UvmState("loaded");
        INITIALIZED = new UvmState("initialized");
        RUNNING = new UvmState("running");
        DESTROYED = new UvmState("destroyed");

        Map<String, UvmState> m = new HashMap<String, UvmState>(4);
        m.put(LOADED.toString(), LOADED);
        m.put(INITIALIZED.toString(), INITIALIZED);
        m.put(RUNNING.toString(), RUNNING);
        m.put(DESTROYED.toString(), DESTROYED);

        INSTANCES = Collections.unmodifiableMap(m);
    }

    private final String state;

    public static UvmState getInstance(String state)
    {
        return (UvmState)INSTANCES.get(state);
    }

    private UvmState(String state) { this.state = state; }

    public String toString() { return state; }

    // Serialization ------------------------------------------------------

    Object readResolve()
    {
        return getInstance(state);
    }
}
