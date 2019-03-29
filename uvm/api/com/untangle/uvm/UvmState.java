/**
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
 */
@SuppressWarnings("serial")
public class UvmState implements Serializable, org.json.JSONString
{

    /**
     * Booted, but not initialized. This is a transient state, just
     * after the world has been instantiated, but before
     * UvmContext.init() has been called.
     */
    public static final UvmState LOADED;

    /**
     * Initialized, but not running. We've run init() but not yet started
     * the apps or Tomcat.
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

        Map<String, UvmState> m = new HashMap<>(4);
        m.put(LOADED.toString(), LOADED);
        m.put(INITIALIZED.toString(), INITIALIZED);
        m.put(RUNNING.toString(), RUNNING);
        m.put(DESTROYED.toString(), DESTROYED);

        INSTANCES = Collections.unmodifiableMap(m);
    }

    private final String state;

    public static UvmState getInstance(String state)
    {
        return INSTANCES.get(state);
    }

    private UvmState(String state) { this.state = state; }

    public String toString() { return state; }

    // Serialization ------------------------------------------------------

    Object readResolve()
    {
        return getInstance(state);
    }
    
    public String toJSONString()
    {
        org.json.JSONObject jO = new org.json.JSONObject(this);
        return jO.toString();
    }
}
