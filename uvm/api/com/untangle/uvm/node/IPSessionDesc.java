/*
 * $Id$
 */
package com.untangle.uvm.node;

public interface IPSessionDesc extends SessionEndpoints
{
    /**
     * <code>id</code> returns the session's unique identifier, a positive integer >= 1.
     * All sessions have a unique id assigned by Argon.  This will eventually, of course,
     * wrap around.  This will take long enough, and any super-long-lived sessions that
     * get wrapped to will not be duplicated, so the rollover is ok.
     *
     * @return an <code>int</code> giving the unique ID of the session.
     */
    long id();

    /**
     * User identified for the session.  May be null, which means
     * that no user could be idenitifed for the session.
     *
     */
    String user();
}
