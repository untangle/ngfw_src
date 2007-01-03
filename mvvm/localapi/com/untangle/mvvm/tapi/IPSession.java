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

package com.untangle.mvvm.tapi;

import com.untangle.mvvm.tran.PipelineEndpoints;
import java.net.InetAddress;
import java.nio.ByteBuffer;

/**
 * The interface <code>Session</code> here.
 *
 * @author <a href="mailto:jdi@untangle.com"></a>
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
     * <code>release</code> releases all interest in all non-final events for this session.  Only
     * the finalization event will be delievered, when the resulting session ends.
     *
     * This call is only valid while in NORMAL_MODE.
     * Note: Just calls release(true);
     *
     */
    void release();

    /**
     * <code>release</code> notifies the TAPI that this session may continue,
     * but no data events will be delivered for
     * the session.  If needsFinalization is false, no further events will be delivered for the session
     * at all.  IF needsFinalization is true, then the only event that will be delivered is a Finalization
     * event when the resulting session ends.
     *
     * @param needsFinalization a <code>boolean</code> true if the transform needs a finalization event when the released session ends.
     */
    void release(boolean needsFinalization);

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

    PipelineEndpoints pipelineEndpoints();
}
