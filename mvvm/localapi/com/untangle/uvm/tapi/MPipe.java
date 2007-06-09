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

import com.untangle.mvvm.tran.Transform;


/**
 * The <code>MPipe</code> interface represents an active MetaPipe.
 * Most transforms only have one active <code>MPipe</code> at a time,
 * the rest have exactly 2 (casings).
 *
 * This class's instances represent and contain the subscription
 * state, pipeline state, and accessors to get the live sessions for
 * the pipe, as well as
 *
 * This used to be called 'Xenon'.
 *
 * @author <a href="mailto:jdi@untangle.com"></a>
 * @version 1.0
 */
public interface MPipe {

    /**
     * Deactivates an active MetaPipe and disconnects it from argon.
     * This kills all sessions and threads, and keeps any new sessions
     * or further commands from being issued.
     *
     * The xenon may not be used again.  State will be
     * <code>DEAD_ARGON</code> from here on out.
     */
    void destroy();

    PipeSpec getPipeSpec();

    int[] liveSessionIds();

    IPSessionDesc[] liveSessionDescs();

    void dumpSessions();

    Transform transform();

    // disconnect?
    // void closeClientChannel(TCPSession session);
    // void closeServerChannel(TCPSession session);

    // void scheduleTimer(IPSessionImpl session, long delay);
    // void cancelTimer(IPSessionImpl session);
}


