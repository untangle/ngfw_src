/**
 * $Id$
 */
package com.untangle.uvm.argon;


/**
 * The interface <code>Session</code> here.
 */
public interface ArgonIPSession extends ArgonSession
{
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
