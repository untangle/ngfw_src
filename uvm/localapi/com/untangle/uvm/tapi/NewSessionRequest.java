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

package com.untangle.uvm.tapi;

public interface NewSessionRequest 
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
     * <code>mPipe</code> returns the Meta Pipe <code>MPipe</code> that this session lives on.
     *
     * @return the <code>MPipe</code> that this session is for
     */
    MPipe mPipe();

    /**
     * Attaches the given object to this session request.
     *
     * <p> An attached object may later be retrieved via the {@link #attachment
     * attachment} method of the session itself.  (The attached object is automatically
     * attached to the session, assuming the request is allowed).
     * Only one object may be attached at a time; invoking
     * this method causes any previous attachment to be discarded.  The current
     * attachment may be discarded by attaching <tt>null</tt>.  </p>
     *
     * @param  ob
     *         The object to be attached; may be <tt>null</tt>
     *
     * @return  The previously-attached object, if any,
     *          otherwise <tt>null</tt>
     */
    Object attach(Object ob);

    /**
     * Retrieves the current attachment.  </p>
     *
     * @return  The object currently attached to this session,
     *          or <tt>null</tt> if there is no attachment
     */
    Object attachment();
}
