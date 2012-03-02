/**
 * $Id$
 */
package com.untangle.uvm.vnet;

import com.untangle.uvm.node.SessionEvent;

/**
 * The IP Session interface
 */
public interface IPSession extends VnetSessionDesc, Session
{
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
     * <code>orClientMark</code> bitwise ORs the provided bitmask with the current client-side conn-mark
     */
    void orClientMark(int bitmask);

    /**
     * <code>setClientQosMark</code> sets the connmark so this session' client-side packets get the provided QoS priority
     */
    void setClientQosMark(int priority);
    
    /**
     * <code>serverMark</code> returns the server-side socket mark for this session
     */
    int  serverMark();

    /**
     * <code>serverMark</code> sets the server-side socket mark for this session
     */
    void serverMark(int newmark);

    /**
     * <code>orServerMark</code> bitwise ORs the provided bitmask with the current server-side conn-mark
     */
    void orServerMark(int bitmask);

    /**
     * <code>setServerQosMark</code> sets the connmark so this session' server-side packets get the provided QoS priority
     */
    void setServerQosMark(int priority);
    
    /**
     * Get the pipeline endpoints for this session
     */
    SessionEvent sessionEvent();

    
}
