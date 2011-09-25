/*
 * $Id: VnetSessionDesc.java,v 1.00 2011/09/24 23:25:47 dmorris Exp $
 */
package com.untangle.uvm.vnet;

import com.untangle.uvm.node.SessionEndpoints;

/**
 * IP Session description interface
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
public interface VnetSessionDesc extends com.untangle.uvm.node.IPSessionDesc
{
    static final byte CLOSED = 0;
    static final byte EXPIRED = 0;
    static final byte OPEN = 4;
    static final byte HALF_OPEN_INPUT = 5; /* for TCP */
    static final byte HALF_OPEN_OUTPUT = 6; /* for TCP */

    byte clientState();
    byte serverState();

    /**
     * <code>id</code> returns the session's unique identifier, a positive integer >= 1.
     * All sessions have a unique id assigned by Argon.  This will eventually, of course,
     * wrap around.  This will take long enough, and any super-long-lived sessions that
     * get wrapped to will not be duplicated, so the rollover is ok.
     *
     * @return an <code>int</code> giving the unique ID of the session.
     */
    //int id();

    /**
     * The <code>stats</code> method returns statistics for this session.
     *
     * @return a <code>SessionStats</code> giving the current statistics for this session
     */
    SessionStats stats();
    
}
