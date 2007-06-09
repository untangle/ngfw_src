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

// import org.apache.commons.jxpath.JXPathContext;

/**
 * The interface <code>Session</code> here.
 *
 * @author <a href="mailto:jdi@untangle.com"></a>
 * @version 1.0
 */
public interface Session extends SessionDesc {

    /**
     * <code>mPipe</code> returns the Meta Pipe <code>MPipe</code> that this session lives on.
     *
     * @return the <code>MPipe</code> that this session is for
     */
    MPipe mPipe();

    /**
     * Attaches the given object to this session.
     *
     * <p> An attached object may later be retrieved via the {@link #attachment
     * attachment} method.  Only one object may be attached at a time; invoking
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

    // JXPathContext sessionContext();

    // ExtendedPreferences sessionNode();

}
