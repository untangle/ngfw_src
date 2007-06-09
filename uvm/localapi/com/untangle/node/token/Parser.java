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

import java.nio.ByteBuffer;

public interface Parser
{
    /**
     * Parse data from the stream.
     *
     * @param chunk the byte data from the stream.
     * @return the ParseResult.
     * @exception ParseException if a parse error occurs.
     */
    ParseResult parse(ByteBuffer chunk) throws ParseException;

    /**
     * Called with last data from the read buffer on session close.
     *
     * @param chunk data from read buffer.
     * @return the ParseResult.
     * @exception ParseException if a parse error occurs.
     */
    ParseResult parseEnd(ByteBuffer chunk) throws ParseException;

    /**
     * On FIN, allows the parser to stream out any final data.
     *
     * XXX this is pretty ugly, I should allow a ParseResult and
     * stream it in the adapt or if necessary.
     *
     * @return a <code>TokenStreamer</code> value
     */
    TokenStreamer endSession();

    /**
     * Called on scheduled timer event.
     */
    void handleTimer();

    /**
     * Called when both client and server sides 
     * {@link com.untangle.mvvm.tapi.event.SessionEventListener#handleTCPFinalized are shutdown}
     */
    void handleFinalized();
}
