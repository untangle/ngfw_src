/**
 * $Id: HookCallback.java,v 1.00 2016/10/28 11:00:52 dmorris Exp $
 */
package com.untangle.uvm;

/**
 * A generic interface to define hook callback functions.
 */
public interface HookCallback
{
    /**
     * Returns the hook callback name.
     * This is a unique identifier and must be unique.
     * This is used for logging and to avoid duplicates.
     */
    public String getName();

    /**
     * This is the callback called when the "event" occurs
     */
    public void callback( Object... arguments );
}
