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

package com.untangle.tran.token;

import com.untangle.mvvm.tapi.event.TCPStreamer;

/**
 * An Unparser transforms tokens into bytes.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public interface Unparser
{
    /**
     * Transform tokens back into bytes.
     *
     * @param token next token.
     * @return UnparseResult containing byte content of token.
     * @exception UnparseException on unparse error.
     */
    UnparseResult unparse(Token token) throws UnparseException;

    /**
     * Called when a session is being released. The unparser should
     * return any queued data.
     *
     * @return Unparse result containing queued data.
     * @exception UnparseException if thrown, it will cause the
     * session to be closed.
     */
    UnparseResult releaseFlush() throws UnparseException;

    /**
     * On session end, the unparser has an opportunity to stream data.
     *
     * @return TokenStreamer that streams the final data.
     */
    TCPStreamer endSession();

    /**
     * Called when both client and server sides 
     * {@link com.untangle.mvvm.tapi.event.SessionEventListener#handleTCPFinalized are shutdown}
     */
    void handleFinalized();    
}
