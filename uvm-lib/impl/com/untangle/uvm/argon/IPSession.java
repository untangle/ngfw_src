/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.argon;


/**
 * The interface <code>Session</code> here.
 *
 * @author <a href="mailto:jdi@untangle.com"></a>
 * @version 1.0
 */

public interface IPSession extends ArgonSession, IPSessionDesc  {
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
