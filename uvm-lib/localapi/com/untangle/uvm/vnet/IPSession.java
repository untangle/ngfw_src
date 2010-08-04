/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.uvm.vnet;

import com.untangle.uvm.node.PipelineEndpoints;

/**
 * The IP Session interface
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
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
     * @param needsFinalization a <code>boolean</code> true if the node needs a finalization event when the released session ends.
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

    /**
     * <code>clientMark</code> returns the server-side socket mark for this session
     */
    int  clientMark();

    /**
     * <code>clientMark</code> sets the server-side socket mark for this session
     */
    void clientMark(int newmark);

    /**
     * <code>serverMark</code> returns the server-side socket mark for this session
     */
    int  serverMark();

    /**
     * <code>serverMark</code> sets the server-side socket mark for this session
     */
    void serverMark(int newmark);

    /**
     * Get the pipeline endpoints for this session
     */
    PipelineEndpoints pipelineEndpoints();

}
