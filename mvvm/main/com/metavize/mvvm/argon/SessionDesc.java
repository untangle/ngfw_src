/*
 * Copyright (c) 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: SessionDesc.java,v 1.3 2005/01/31 03:18:39 rbscott Exp $
 */

package com.metavize.mvvm.argon;

public interface SessionDesc {

    /**
     * <code>id</code> returns the session's unique identifier, a positive integer >= 1.
     * All sessions have a unique id assigned by Argon.  This will eventually, of course,
     * wrap around.  This will take long enough, and any super-long-lived sessions that
     * get wrapped to will not be duplicated, so the rollover is ok.
     *
     * @return an <code>int</code> giving the unique ID of the session.
     */
    int id();

    /**
     * Number of bytes received from the client.
     */
    long c2tBytes();

    /**
     * Number of bytes transmitted to the server.
     */
    long t2sBytes();

    /**
     * Number of bytes received from the server.
     */
    long s2tBytes();
    
    /**
     * Number of bytes transmitted to the client.
     */
    long t2cBytes();

    /**
     * Number of chunks received from the client.
     */
    long c2tChunks();

    /**
     * Number of chunks transmitted to the server.
     */
    long t2sChunks();

    /**
     * Number of chunks received from the server.
     */
    long s2tChunks();
    
    /**
     * Number of chunks transmitted to the client.
     */
    long t2cChunks();
}

