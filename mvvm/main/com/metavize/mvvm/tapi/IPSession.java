/*
 * Copyright (c) 2003, 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: IPSession.java,v 1.7 2005/01/06 02:39:41 jdi Exp $
 */

package com.metavize.mvvm.tapi;

import java.net.InetAddress;
import java.nio.ByteBuffer;

/**
 * The interface <code>Session</code> here.
 *
 * @author <a href="mailto:jdi@SLAB"></a>
 * @version 1.0
 */
public interface IPSession extends IPSessionDesc, Session  {
    
    // Closes both sides
    // void close();

    /**
     * <code>closeClient</code> closes the connection to the client (both input and output).
     * It will result in {TCP,UDP}ClientClosedEvent being delivered for the given side, if one
     * has not already been delivered.  If the client is already closed, this is a no-op.
     *
     * This call is only valid while in NORMAL_MODE.
     *
     * For UDP, this could also be called expireClient(). XX
     *
     */
    // void closeClient();

    // void closeClient(boolean force);

    /**
     * <code>closeServer</code> closes the connection to the server (both input and output).
     * It will result in {TCP,UDP}ServerClosedEvent being delivered for the given side, if one
     * has not already been delivered.  If the server is already closed, this is a no-op.
     *
     * This call is only valid while in NORMAL_MODE.
     *
     * For TCP, this could also be called expireClient(). XX
     *
     */
    // void closeServer();

    // void closeServer(boolean force);

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
