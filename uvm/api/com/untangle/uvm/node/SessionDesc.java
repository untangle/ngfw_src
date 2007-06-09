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

package com.untangle.uvm.node;

public interface SessionDesc
{
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


    /**
     * User identified for the session.  May be null, which means
     * that no user could be idenitifed for the session.
     *
     */
    String user();
}

