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

package com.untangle.uvm.argon;

import java.net.InetAddress;
import java.nio.ByteBuffer;

/**
 * The interface <code>Session</code> here.
 *
 * @author <a href="mailto:jdi@untangle.com"></a>
 * @version 1.0
 */

public interface IPSession extends Session, IPSessionDesc  {
    // Closes both sides
    // void close();

    /**
     * <code>closeClient</code> closes the connection to the client (both input and output).
     *
     * For UDP, this could also be called expireClient(). XX
     *
     */
    // void closeClient();

    /**
     * <code>closeServer</code> closes the connection to the server (both input and output).
     *
     * For TCP, this could also be called expireClient(). XX
     *
     */
    // void closeServer();

    /**
     * <code>release</code> releases all interest in further events for this session.
     *
     * This call is only valid while in NORMAL_MODE.
     *
     */
    void release();

    /**
     * <code>scheduleTimer</code> sets the timer for this session to fire in
     * the given number of milliseconds. If the timer is already scheduled, it
     * the existing delay is discarded and the timer is rescheduled for the new
     * <code>delay</code>.
     *
     * @param delay a <code>long</code> giving milliseconds until the timer is to fire
     * @exception IllegalArgumentException if the delay is negative
     */
    void scheduleTimer(long delay) throws IllegalArgumentException;

    /**
     * <code>cancelTimer</code> cancels any scheduled timer expiration for this session.
     *
     */
    void cancelTimer();
}
